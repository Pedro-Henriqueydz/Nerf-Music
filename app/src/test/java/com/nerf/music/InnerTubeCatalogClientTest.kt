package com.nerf.music

import com.nerf.music.data.model.SearchFilter
import com.nerf.music.data.model.SearchResult
import com.nerf.music.data.model.Track
import com.nerf.music.data.network.InnerTubeCatalogClient
import com.nerf.music.data.repository.MusicRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InnerTubeCatalogClientTest {

    private lateinit var client: InnerTubeCatalogClient

    @Before
    fun setup() {
        client = InnerTubeCatalogClient()
    }

    private fun wrapInInnerTubeEnvelope(sectionsJson: String): String {
        return """
        {
          "contents": {
            "tabbedSearchResultsRenderer": {
              "tabs": [
                {
                  "tabRenderer": {
                    "content": {
                      "sectionListRenderer": {
                        "contents": [
                          $sectionsJson
                        ]
                      }
                    }
                  }
                }
              ]
            }
          }
        }
        """.trimIndent()
    }

    @Test
    fun testEmptyQueryReturnsEmptySearchResult() = runBlocking {
        val result = client.search("   ")
        assertTrue("Busca com espaços vazios deve retornar sucesso", result.isSuccess)
        val searchResult = result.getOrNull()
        assertNotNull("SearchResult não pode ser nulo", searchResult)
        assertTrue("Busca vazia deve ter resultado vazio", searchResult!!.isEmpty)
    }

    @Test
    fun testEmptyAndMalformedResponsesDoNotCrash() {
        val emptyResult = client.parseSearchResponse("")
        assertTrue("String vazia deve retornar SearchResult vazio", emptyResult.isEmpty)

        val malformedResult = client.parseSearchResponse("{ invalid json: [1, 2, }")
        assertTrue("JSON corrompido deve retornar SearchResult vazio sem lançar exceção", malformedResult.isEmpty)

        val unexpectedStructure = client.parseSearchResponse("""{"foo": "bar", "contents": {}}""")
        assertTrue("Estrutura inesperada deve ser tratada com segurança", unexpectedStructure.isEmpty)
    }

    @Test
    fun testArtistResultsAreNotMistakenForSongs() {
        val fixture = wrapInInnerTubeEnvelope("""
        {
          "musicCardShelfRenderer": {
            "title": { "runs": [{ "text": "Elis Regina" }] },
            "subtitle": { "runs": [{ "text": "Artista" }, { "text": " • " }, { "text": "2,63 mi ouvintes" }] },
            "thumbnail": {
              "musicThumbnailRenderer": {
                "thumbnail": {
                  "thumbnails": [{ "url": "https://lh3.googleusercontent.com/elis.jpg" }]
                }
              }
            }
          }
        },
        {
          "itemSectionRenderer": {
            "contents": [
              {
                "musicResponsiveListItemRenderer": {
                  "flexColumns": [
                    {
                      "musicResponsiveListItemFlexColumnRenderer": {
                        "text": { "runs": [{ "text": "Elis Regina" }] }
                      }
                    },
                    {
                      "musicResponsiveListItemFlexColumnRenderer": {
                        "text": { "runs": [{ "text": "Artista" }] }
                      }
                    }
                  ],
                  "thumbnail": {
                    "musicThumbnailRenderer": {
                      "thumbnail": {
                        "thumbnails": [{ "url": "https://lh3.googleusercontent.com/elis2.jpg" }]
                      }
                    }
                  }
                }
              }
            ]
          }
        }
        """)

        val result = client.parseSearchResponse(fixture)

        assertNotNull("TopArtist deve ser extraído", result.topArtist)
        assertEquals("Elis Regina", result.topArtist?.name)
        assertTrue("Subtítulo do artista deve ser preservado", result.topArtist?.subtitle?.contains("Artista") == true)
        assertEquals("https://lh3.googleusercontent.com/elis.jpg", result.topArtist?.thumbnailUrl)

        // Artista não pode ser confundido com música
        assertTrue("Nenhum artista deve ser colocado na lista de músicas", result.songs.isEmpty())
        assertTrue("A lista de artistas deve conter o artista", result.artists.isNotEmpty() || result.topArtist != null)
    }

    @Test
    fun testSongResultsAreParsedCorrectly() {
        val fixture = wrapInInnerTubeEnvelope("""
        {
          "itemSectionRenderer": {
            "contents": [
              {
                "musicResponsiveListItemRenderer": {
                  "flexColumns": [
                    {
                      "musicResponsiveListItemFlexColumnRenderer": {
                        "text": { "runs": [{ "text": "Como Nossos Pais" }] }
                      }
                    },
                    {
                      "musicResponsiveListItemFlexColumnRenderer": {
                        "text": {
                          "runs": [
                            { "text": "Música" },
                            { "text": " • " },
                            { "text": "Elis Regina" },
                            { "text": " • " },
                            { "text": "Falso Brilhante" },
                            { "text": " • " },
                            { "text": "04:38" }
                          ]
                        }
                      }
                    }
                  ],
                  "overlay": {
                    "musicItemThumbnailOverlayRenderer": {
                      "content": {
                        "musicPlayButtonRenderer": {
                          "playNavigationEndpoint": {
                            "watchEndpoint": {
                              "videoId": "vid_como_nossos_pais"
                            }
                          }
                        }
                      }
                    }
                  },
                  "thumbnail": {
                    "musicThumbnailRenderer": {
                      "thumbnail": {
                        "thumbnails": [
                          { "url": "https://lh3.googleusercontent.com/small.jpg" },
                          { "url": "https://lh3.googleusercontent.com/large.jpg" }
                        ]
                      }
                    }
                  }
                }
              }
            ]
          }
        }
        """)

        val result = client.parseSearchResponse(fixture)

        assertEquals("Deve extrair exatamente 1 música", 1, result.songs.size)
        val song = result.songs.first()
        assertEquals("vid_como_nossos_pais", song.id)
        assertEquals("Como Nossos Pais", song.title)
        assertEquals("Elis Regina", song.artist)
        assertEquals("Falso Brilhante", song.album)
        assertEquals("04:38", song.durationText)
        assertEquals("https://lh3.googleusercontent.com/large.jpg", song.thumbnailUrl)
        assertEquals("INNERTUBE_WEB_REMIX", song.sourceIdentifier)
    }

    @Test
    fun testMultipleSongsFromMusicShelfRenderer() {
        val fixture = wrapInInnerTubeEnvelope("""
        {
          "musicShelfRenderer": {
            "contents": [
              {
                "musicResponsiveListItemRenderer": {
                  "flexColumns": [
                    { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "Águas de Março" }] } } },
                    { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "Elis Regina" }, { "text": " • " }, { "text": "03:32" }] } } }
                  ],
                  "overlay": {
                    "musicItemThumbnailOverlayRenderer": {
                      "content": {
                        "musicPlayButtonRenderer": {
                          "playNavigationEndpoint": {
                            "watchEndpoint": { "videoId": "vid_aguas" }
                          }
                        }
                      }
                    }
                  }
                }
              },
              {
                "musicResponsiveListItemRenderer": {
                  "flexColumns": [
                    { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "O Bêbado e a Equilibrista" }] } } },
                    { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "Elis Regina" }, { "text": " • " }, { "text": "03:47" }] } } }
                  ],
                  "overlay": {
                    "musicItemThumbnailOverlayRenderer": {
                      "content": {
                        "musicPlayButtonRenderer": {
                          "playNavigationEndpoint": {
                            "watchEndpoint": { "videoId": "vid_bebado" }
                          }
                        }
                      }
                    }
                  }
                }
              },
              {
                "musicResponsiveListItemRenderer": {
                  "flexColumns": [
                    { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "Fascinação" }] } } },
                    { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "Elis Regina" }, { "text": " • " }, { "text": "03:05" }] } } }
                  ],
                  "overlay": {
                    "musicItemThumbnailOverlayRenderer": {
                      "content": {
                        "musicPlayButtonRenderer": {
                          "playNavigationEndpoint": {
                            "watchEndpoint": { "videoId": "vid_fascinacao" }
                          }
                        }
                      }
                    }
                  }
                }
              }
            ]
          }
        }
        """)

        val result = client.parseSearchResponse(fixture)

        assertEquals("Deve extrair 3 músicas da estante", 3, result.songs.size)
        assertEquals("Águas de Março", result.songs[0].title)
        assertEquals("vid_aguas", result.songs[0].id)
        assertEquals("O Bêbado e a Equilibrista", result.songs[1].title)
        assertEquals("Fascinação", result.songs[2].title)
    }

    @Test
    fun testMissingDurationsAndThumbnailsHandledSafely() {
        val fixture = wrapInInnerTubeEnvelope("""
        {
          "musicShelfRenderer": {
            "contents": [
              {
                "musicResponsiveListItemRenderer": {
                  "flexColumns": [
                    { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "Música Sem Duração Nem Capa" }] } } },
                    { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "Música" }, { "text": " • " }, { "text": "Artista Demo" }] } } }
                  ]
                }
              }
            ]
          }
        }
        """)

        val result = client.parseSearchResponse(fixture)

        assertEquals("Deve processar 1 música mesmo sem capa ou duração", 1, result.songs.size)
        val song = result.songs.first()
        assertEquals("Música Sem Duração Nem Capa", song.title)
        assertEquals("Artista Demo", song.artist)
        assertNull("Thumbnail deve ser null quando ausente", song.thumbnailUrl)
        assertEquals("--:--", song.durationText)
        assertTrue("ID gerado deve ser não vazio", song.id.isNotBlank())
    }

    @Test
    fun testLiveNetworkCatalogSearchIfAvailable() = runBlocking {
        try {
            val result = client.search("Elis Regina", filter = SearchFilter.ALL)
            if (result.isSuccess) {
                val data = result.getOrNull()
                assertNotNull("Resultado não deve ser nulo", data)
                println("Busca online bem-sucedida. Músicas: ${data?.songs?.size}, Artistas: ${data?.artists?.size}, Top: ${data?.topArtist?.name}")
                data?.songs?.take(5)?.forEach {
                    println("TRACK_TEST: id=${it.id} | title=${it.title} | artist=${it.artist}")
                }
                // Se conectado, deve encontrar o artista ou músicas
                assertTrue(
                    "Busca por Elis Regina online deve retornar artista ou músicas",
                    data!!.songs.isNotEmpty() || data.topArtist != null || data.artists.isNotEmpty()
                )
            }
        } catch (e: Exception) {
            println("Aviso: Teste de rede ignorado por indisponibilidade de conexão: ${e.message}")
        }
    }

    @Test
    fun testCardShelfNestedSongsAndDurationNormalization() {
        val fixture = wrapInInnerTubeEnvelope("""
        {
          "musicCardShelfRenderer": {
            "title": { "runs": [{ "text": "Altare" }] },
            "subtitle": { "runs": [{ "text": "Artista" }, { "text": " • " }, { "text": "24,1 mi ouvintes mensais" }] },
            "contents": [
              {
                "musicResponsiveListItemRenderer": {
                  "flexColumns": [
                    { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "Stay" }] } } },
                    { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "Música" }, { "text": " • " }, { "text": "3:47" }] } } }
                  ],
                  "overlay": {
                    "musicItemThumbnailOverlayRenderer": {
                      "content": {
                        "musicPlayButtonRenderer": {
                          "playNavigationEndpoint": {
                            "watchEndpoint": { "videoId": "3U4wZkM-LQo" }
                          }
                        }
                      }
                    }
                  }
                }
              }
            ]
          }
        },
        {
          "musicShelfRenderer": {
            "contents": [
              {
                "musicResponsiveListItemRenderer": {
                  "flexColumns": [
                    { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "want your love (slowed)" }] } } },
                    {
                      "musicResponsiveListItemFlexColumnRenderer": {
                        "text": {
                          "runs": [
                            { "text": "Altare" },
                            { "text": " e " },
                            { "text": "almogfx" },
                            { "text": " • " },
                            { "text": "want your love" },
                            { "text": " • " },
                            { "text": "2:52" }
                          ]
                        }
                      }
                    }
                  ],
                  "playlistItemData": { "videoId": "98bMdPmEA-w" }
                }
              }
            ]
          }
        }
        """)

        val result = client.parseSearchResponse(fixture)
        assertNotNull(result.topArtist)
        assertEquals("Altare", result.topArtist?.name)
        assertEquals(2, result.songs.size)

        val topSong = result.songs[0]
        assertEquals("Stay", topSong.title)
        assertEquals("Altare", topSong.artist)
        assertEquals("03:47", topSong.durationText)

        val secondSong = result.songs[1]
        assertEquals("want your love (slowed)", secondSong.title)
        assertEquals("Altare e almogfx", secondSong.artist)
        assertEquals("02:52", secondSong.durationText)
    }

    @Test
    fun testExactMatchRelevanceBoostPrioritizesAltareOverAltares() {
        val repo = MusicRepositoryImpl(client)
        val raw = SearchResult(
            songs = listOf(
                Track(id = "1", title = "Tu amor me basta", artist = "Altares", durationText = "07:32"),
                Track(id = "2", title = "Altar", artist = "Ashes of Eden", durationText = "03:32"),
                Track(id = "3", title = "Stay", artist = "Altare", durationText = "03:47"),
                Track(id = "4", title = "Time", artist = "Dravyn e Altare", durationText = "03:21")
            )
        )

        val prioritized = repo.prioritizeSearchResults(raw, "Altare")
        assertEquals("Stay (Altare exato) deve ser o primeiro", "Stay", prioritized.songs[0].title)
        assertEquals("Time (colaboração exata Altare) deve ser o segundo", "Time", prioritized.songs[1].title)
        assertEquals("Tu amor me basta (Altares parcial) deve vir depois", "Tu amor me basta", prioritized.songs[2].title)
        assertEquals("Altar (fuzzy) deve vir por último", "Altar", prioritized.songs[3].title)
    }

    @Test
    fun testNextPayloadAndDynamicRadioParsing() {
        val payload = client.buildNextPayload("3U4wZkM-LQo", params = null).toString()
        assertTrue("Payload /next deve conter playlistId RDAMVM+videoId", payload.contains("RDAMVM3U4wZkM-LQo"))

        val nextResponseFixture = """
        {
          "contents": {
            "singleColumnMusicWatchNextResultsRenderer": {
              "tabbedRenderer": {
                "watchNextTabbedResultsRenderer": {
                  "tabs": [
                    {
                      "tabRenderer": {
                        "title": "A seguir",
                        "content": {
                          "musicQueueRenderer": {
                            "header": {
                              "musicQueueHeaderRenderer": {
                                "title": { "runs": [{ "text": "Tocando de" }] },
                                "subtitle": { "runs": [{ "text": "Mix de Stay" }] }
                              }
                            },
                            "subHeaderChipCloud": {
                              "chipCloudRenderer": {
                                "chips": [
                                  {
                                    "chipCloudChipRenderer": {
                                      "text": { "runs": [{ "text": "Tudo" }] },
                                      "isSelected": true,
                                      "navigationEndpoint": {
                                        "queueUpdateCommand": {
                                          "fetchContentsCommand": {
                                            "watchEndpoint": {
                                              "playlistId": "RDAMVM3U4wZkM-LQo",
                                              "params": "wAEB"
                                            }
                                          }
                                        }
                                      }
                                    }
                                  },
                                  {
                                    "chipCloudChipRenderer": {
                                      "text": { "runs": [{ "text": "Descobrir" }] },
                                      "isSelected": false,
                                      "navigationEndpoint": {
                                        "queueUpdateCommand": {
                                          "fetchContentsCommand": {
                                            "watchEndpoint": {
                                              "playlistId": "RDATg1x3U4wZkM-LQo",
                                              "params": "ggU0DescobrirParams"
                                            }
                                          }
                                        }
                                      }
                                    }
                                  }
                                ]
                              }
                            },
                            "content": {
                              "playlistPanelRenderer": {
                                "contents": [
                                  {
                                    "playlistPanelVideoRenderer": {
                                      "videoId": "3U4wZkM-LQo",
                                      "title": { "runs": [{ "text": "Stay" }] },
                                      "longBylineText": { "runs": [{ "text": "Altare" }, { "text": " • " }, { "text": "Stay - Single" }] },
                                      "lengthText": { "runs": [{ "text": "3:47" }] },
                                      "thumbnail": {
                                        "thumbnails": [{ "url": "https://lh3.googleusercontent.com/stay.jpg" }]
                                      }
                                    }
                                  },
                                  {
                                    "playlistPanelVideoRenderer": {
                                      "videoId": "98bMdPmEA-w",
                                      "title": { "runs": [{ "text": "want your love (slowed)" }] },
                                      "longBylineText": { "runs": [{ "text": "Altare" }, { "text": " e " }, { "text": "almogfx" }, { "text": " • " }, { "text": "want your love" }] },
                                      "lengthText": { "runs": [{ "text": "2:52" }] }
                                    }
                                  }
                                ]
                              }
                            }
                          }
                        }
                      }
                    }
                  ]
                }
              }
            }
          }
        }
        """.trimIndent()

        val upNext = client.parseNextResponse(nextResponseFixture)
        assertEquals("Mix de Stay", upNext.queueSourceTitle)
        assertEquals(2, upNext.chips.size)
        assertEquals("Tudo", upNext.chips[0].title)
        assertTrue(upNext.chips[0].isSelected)
        assertEquals("Descobrir", upNext.chips[1].title)
        assertEquals("ggU0DescobrirParams", upNext.chips[1].params)
        assertEquals("RDATg1x3U4wZkM-LQo", upNext.chips[1].playlistId)

        assertEquals(2, upNext.tracks.size)
        assertEquals("Stay", upNext.tracks[0].title)
        assertEquals("Altare", upNext.tracks[0].artist)
        assertEquals("03:47", upNext.tracks[0].durationText)
        assertEquals("want your love (slowed)", upNext.tracks[1].title)
        assertEquals("Altare e almogfx", upNext.tracks[1].artist)
        assertEquals("02:52", upNext.tracks[1].durationText)
    }
    @Test
    fun testHomeFeedPayloadAndParsing() {
        val payload = client.buildBrowsePayload("FEmusic_home").toString()
        assertTrue("Payload deve conter browseId FEmusic_home", payload.contains("FEmusic_home"))

        val homeResponseFixture = """
        {
          "contents": {
            "singleColumnBrowseResultsRenderer": {
              "tabs": [
                {
                  "tabRenderer": {
                    "content": {
                      "sectionListRenderer": {
                        "contents": [
                          {
                            "musicCarouselShelfRenderer": {
                              "header": {
                                "musicCarouselShelfBasicHeaderRenderer": {
                                  "title": { "runs": [{ "text": "Escolha a dedo" }] }
                                }
                              },
                              "contents": [
                                {
                                  "musicResponsiveListItemRenderer": {
                                    "flexColumns": [
                                      {
                                        "musicResponsiveListItemFlexColumnRenderer": {
                                          "text": { "runs": [{ "text": "Cadeira Cativa (Ao Vivo)" }] }
                                        }
                                      },
                                      {
                                        "musicResponsiveListItemFlexColumnRenderer": {
                                          "text": {
                                            "runs": [
                                              { "text": "Zé Neto & Cristiano" },
                                              { "text": " • " },
                                              { "text": "Tocou 126 mi vezes" }
                                            ]
                                          }
                                        }
                                      }
                                    ],
                                    "overlay": {
                                      "musicItemThumbnailOverlayRenderer": {
                                        "content": {
                                          "musicPlayButtonRenderer": {
                                            "playNavigationEndpoint": {
                                              "watchEndpoint": { "videoId": "KTy17rny7vI" }
                                            }
                                          }
                                        }
                                      }
                                    },
                                    "thumbnail": {
                                      "musicThumbnailRenderer": {
                                        "thumbnail": {
                                          "thumbnails": [{ "url": "https://lh3.googleusercontent.com/cadeira.jpg" }]
                                        }
                                      }
                                    }
                                  }
                                }
                              ]
                            }
                          },
                          {
                            "musicCarouselShelfRenderer": {
                              "header": {
                                "musicCarouselShelfBasicHeaderRenderer": {
                                  "title": { "runs": [{ "text": "Músicas em alta para você" }] }
                                }
                              },
                              "contents": [
                                {
                                  "musicResponsiveListItemRenderer": {
                                    "flexColumns": [
                                      {
                                        "musicResponsiveListItemFlexColumnRenderer": {
                                          "text": { "runs": [{ "text": "Calma" }] }
                                        }
                                      },
                                      {
                                        "musicResponsiveListItemFlexColumnRenderer": {
                                          "text": {
                                            "runs": [
                                              { "text": "Marília Mendonça" }
                                            ]
                                          }
                                        }
                                      }
                                    ],
                                    "overlay": {
                                      "musicItemThumbnailOverlayRenderer": {
                                        "content": {
                                          "musicPlayButtonRenderer": {
                                            "playNavigationEndpoint": {
                                              "watchEndpoint": { "videoId": "t5TnLL1AhOY" }
                                            }
                                          }
                                        }
                                      }
                                    }
                                  }
                                }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  }
                }
              ]
            }
          }
        }
        """.trimIndent()

        val sections = client.parseHomeFeedResponse(homeResponseFixture)
        assertEquals(2, sections.size)
        assertEquals("Escolha a dedo", sections[0].title)
        assertEquals(1, sections[0].items.size)
        assertEquals("Cadeira Cativa (Ao Vivo)", sections[0].items[0].title)
        assertEquals("Zé Neto & Cristiano", sections[0].items[0].artist)
        assertEquals("KTy17rny7vI", sections[0].items[0].id)
        assertEquals("https://lh3.googleusercontent.com/cadeira.jpg", sections[0].items[0].thumbnailUrl)

        assertEquals("Músicas em alta para você", sections[1].title)
        assertEquals(1, sections[1].items.size)
        assertEquals("Calma", sections[1].items[0].title)
        assertEquals("Marília Mendonça", sections[1].items[0].artist)
        assertEquals("t5TnLL1AhOY", sections[1].items[0].id)
    }
}