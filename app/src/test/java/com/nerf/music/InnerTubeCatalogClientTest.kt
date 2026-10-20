package com.nerf.music

import com.nerf.music.data.model.SearchFilter
import com.nerf.music.data.network.InnerTubeCatalogClient
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
}
