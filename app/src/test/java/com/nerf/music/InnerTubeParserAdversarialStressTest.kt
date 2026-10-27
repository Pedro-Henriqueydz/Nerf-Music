package com.nerf.music

import com.nerf.music.data.model.HomeSectionStyle
import com.nerf.music.data.network.InnerTubeCatalogClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Empirical adversarial stress test suite for [InnerTubeCatalogClient].
 *
 * Stress-tests:
 * 1. Non-English tab parsing and canonical browseId extraction (MPTR..., FEmusic_related...).
 * 2. Japanese anime bands (e.g. Kessoku Band from Bocchi the Rock) with localized title runs.
 * 3. Degenerate, empty, and malformed tabs and container envelopes.
 * 4. Nested browse result structures: singleColumnBrowseResultsRenderer vs twoColumnBrowseResultsRenderer.
 * 5. Anime band bio fallback shelf when carousels are empty.
 */
class InnerTubeParserAdversarialStressTest {

    private lateinit var client: InnerTubeCatalogClient

    @Before
    fun setUp() {
        client = InnerTubeCatalogClient()
    }

    // =========================================================================
    // 1. NON-ENGLISH TABS & CANONICAL BROWSE ID EXTRACTION (MPTR..., FEmusic_related...)
    // =========================================================================

    @Test
    fun testJapaneseAnimeBandKessokuBandBrowseIdExtraction() {
        // Simulates YouTube Music response when playing Kessoku Band (Bocchi the Rock!) in Japanese locale
        val fixture = """
        {
          "contents": {
            "singleColumnMusicWatchNextResultsRenderer": {
              "tabbedRenderer": {
                "watchNextTabbedResultsRenderer": {
                  "tabs": [
                    {
                      "tabRenderer": {
                        "title": { "runs": [{ "text": "キュー" }] },
                        "endpoint": { "browseEndpoint": { "browseId": "VLPL_queue_kessoku" } }
                      }
                    },
                    {
                      "tabRenderer": {
                        "title": { "runs": [{ "text": "歌詞" }] },
                        "endpoint": { "browseEndpoint": { "browseId": "MPLYseishun_complex_lyrics" } }
                      }
                    },
                    {
                      "tabRenderer": {
                        "title": { "runs": [{ "text": "結束バンド" }, { "text": " • " }, { "text": "関連" }] },
                        "endpoint": { "browseEndpoint": { "browseId": "MPTRkessoku_band_bocchi_rock" } }
                      }
                    }
                  ]
                }
              }
            }
          }
        }
        """.trimIndent()

        val result = client.parseNextResponse(fixture)
        assertEquals("MPTRkessoku_band_bocchi_rock", result.relatedBrowseId)
        assertEquals("MPLYseishun_complex_lyrics", result.lyricsBrowseId)
    }

    @Test
    fun testCanonicalFEmusicRelatedWithJapaneseLocaleTitle() {
        val fixture = """
        {
          "contents": {
            "singleColumnMusicWatchNextResultsRenderer": {
              "tabbedRenderer": {
                "watchNextTabbedResultsRenderer": {
                  "tabs": [
                    {
                      "tabRenderer": {
                        "title": { "runs": [{ "text": "関連曲" }] },
                        "endpoint": { "browseEndpoint": { "browseId": "FEmusic_related_anime_playlist_456" } }
                      }
                    },
                    {
                      "tabRenderer": {
                        "title": { "runs": [{ "text": "歌詞" }] },
                        "endpoint": { "browseEndpoint": { "browseId": "FEmusic_lyrics_anime_song_789" } }
                      }
                    }
                  ]
                }
              }
            }
          }
        }
        """.trimIndent()

        val result = client.parseNextResponse(fixture)
        assertEquals("FEmusic_related_anime_playlist_456", result.relatedBrowseId)
        assertEquals("FEmusic_lyrics_anime_song_789", result.lyricsBrowseId)
    }

    @Test
    fun testMultilingualTabTitlesAcrossVariousLocales() {
        // German: "Ähnliche Titel", Russian: "Связанные", Korean: "관련 항목", French: "Titres similaires"
        val testCases = listOf(
            Triple("German", "Ähnliche Titel", "MPTRgerman_mix_1"),
            Triple("Russian", "Связанные", "MPTRrussian_mix_2"),
            Triple("Korean", "관련 항목", "FEmusic_related_korean_3"),
            Triple("French", "Titres similaires", "FEmusic_related_french_4"),
            Triple("Portuguese", "Músicas relacionadas", "GENERIC_BROWSE_ID_PT"),
            Triple("English", "Related songs", "GENERIC_BROWSE_ID_EN")
        )

        for ((lang, title, expectedBrowseId) in testCases) {
            val fixture = """
            {
              "contents": {
                "singleColumnMusicWatchNextResultsRenderer": {
                  "tabbedRenderer": {
                    "watchNextTabbedResultsRenderer": {
                      "tabs": [
                        {
                          "tabRenderer": {
                            "title": "$title",
                            "endpoint": { "browseEndpoint": { "browseId": "$expectedBrowseId" } }
                          }
                        }
                      ]
                    }
                  }
                }
              }
            }
            """.trimIndent()

            val result = client.parseNextResponse(fixture)
            assertEquals(
                "Locale $lang ('$title') failed to extract expected relatedBrowseId",
                expectedBrowseId,
                result.relatedBrowseId
            )
        }
    }

    @Test
    fun testCanonicalPrefixOverridesUnrelatedTabTitle() {
        // Even if the tab title has an unexpected string ("Mixes & Suggestions"),
        // a browseId starting with MPTR or FEmusic_related must be recognized.
        val fixture = """
        {
          "contents": {
            "singleColumnMusicWatchNextResultsRenderer": {
              "tabbedRenderer": {
                "watchNextTabbedResultsRenderer": {
                  "tabs": [
                    {
                      "tabRenderer": {
                        "title": { "runs": [{ "text": "Suplementos e Extras" }] },
                        "endpoint": { "browseEndpoint": { "browseId": "MPTRcanonical_prefix_takes_priority" } }
                      }
                    }
                  ]
                }
              }
            }
          }
        }
        """.trimIndent()

        val result = client.parseNextResponse(fixture)
        assertEquals("MPTRcanonical_prefix_takes_priority", result.relatedBrowseId)
    }

    // =========================================================================
    // 2. MISSING, EMPTY, AND MALFORMED TABS STRESS HANDLING
    // =========================================================================

    @Test
    fun testEmptyAndMalformedNextResponsesGracefulHandling() {
        // Completely empty string
        val emptyRes = client.parseNextResponse("")
        assertNull(emptyRes.relatedBrowseId)
        assertNull(emptyRes.lyricsBrowseId)
        assertTrue(emptyRes.tracks.isEmpty())

        // Blank whitespace
        val whitespaceRes = client.parseNextResponse("   \n\t   ")
        assertNull(whitespaceRes.relatedBrowseId)

        // Invalid JSON
        val malformedRes = client.parseNextResponse("{ not valid json at all ...")
        assertNull(malformedRes.relatedBrowseId)

        // Empty JSON object
        val emptyJsonRes = client.parseNextResponse("{}")
        assertNull(emptyJsonRes.relatedBrowseId)

        // Missing tabbedRenderer
        val missingTabbedRes = client.parseNextResponse("""{"contents": {"singleColumnMusicWatchNextResultsRenderer": {}}}""")
        assertNull(missingTabbedRes.relatedBrowseId)
    }

    @Test
    fun testDegenerateTabsArrayElements() {
        // Tabs array containing empty objects, nulls, and tabs without endpoints
        val fixture = """
        {
          "contents": {
            "singleColumnMusicWatchNextResultsRenderer": {
              "tabbedRenderer": {
                "watchNextTabbedResultsRenderer": {
                  "tabs": [
                    {},
                    { "tabRenderer": {} },
                    { "tabRenderer": { "title": "Random Tab Without Endpoint" } },
                    { "tabRenderer": { "title": "Related", "endpoint": {} } },
                    { "tabRenderer": { "title": "Related", "endpoint": { "browseEndpoint": {} } } },
                    { "tabRenderer": { "title": "Related", "endpoint": { "browseEndpoint": { "browseId": "" } } } },
                    {
                      "tabRenderer": {
                        "title": "Relacionadas Valida",
                        "endpoint": { "browseEndpoint": { "browseId": "MPTRvalid_after_degenerates" } }
                      }
                    }
                  ]
                }
              }
            }
          }
        }
        """.trimIndent()

        val result = client.parseNextResponse(fixture)
        assertEquals("MPTRvalid_after_degenerates", result.relatedBrowseId)
    }

    // =========================================================================
    // 3. NESTED BROWSE RESULT RENDERERS (SingleColumn vs TwoColumn vs Direct)
    // =========================================================================

    @Test
    fun testParseRelatedResponseDirectSectionListRenderer() {
        val fixture = """
        {
          "contents": {
            "sectionListRenderer": {
              "contents": [
                {
                  "musicDescriptionShelfRenderer": {
                    "header": { "runs": [{ "text": "Sobre o artista" }] },
                    "description": { "runs": [{ "text": "Biografia direta." }] }
                  }
                },
                {
                  "musicCarouselShelfRenderer": {
                    "header": {
                      "musicCarouselShelfBasicHeaderRenderer": {
                        "title": { "runs": [{ "text": "Faixas recomendadas" }] }
                      }
                    },
                    "contents": [
                      {
                        "musicResponsiveListItemRenderer": {
                          "flexColumns": [
                            { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "Guitar, Loneliness and Blue Planet" }] } } },
                            { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "Kessoku Band" }] } } }
                          ],
                          "overlay": {
                            "musicItemThumbnailOverlayRenderer": {
                              "content": {
                                "musicPlayButtonRenderer": {
                                  "playNavigationEndpoint": {
                                    "watchEndpoint": { "videoId": "kessoku_guitar_loneliness" }
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
        """.trimIndent()

        val data = client.parseRelatedResponse(fixture)
        assertEquals("Sobre o artista", data.aboutArtistHeader)
        assertEquals("Biografia direta.", data.aboutArtistDescription)
        assertEquals(1, data.sections.size)
        assertEquals("Faixas recomendadas", data.sections[0].title)
        assertEquals("kessoku_guitar_loneliness", data.sections[0].items[0].id)
    }

    @Test
    fun testParseRelatedResponseTwoColumnBrowseResultsWithSecondaryContents() {
        val fixture = """
        {
          "contents": {
            "twoColumnBrowseResultsRenderer": {
              "secondaryContents": {
                "sectionListRenderer": {
                  "contents": [
                    {
                      "musicDescriptionShelfRenderer": {
                        "header": { "runs": [{ "text": "About the Artist" }] },
                        "description": { "runs": [{ "text": "Secondary column bio description." }] }
                      }
                    },
                    {
                      "musicCarouselShelfRenderer": {
                        "numItemsPerColumn": "4",
                        "header": {
                          "musicCarouselShelfBasicHeaderRenderer": {
                            "title": { "runs": [{ "text": "Similar Tracks" }] }
                          }
                        },
                        "contents": [
                          {
                            "musicResponsiveListItemRenderer": {
                              "flexColumns": [
                                { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "Ano Bando" }] } } },
                                { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "Kessoku Band" }] } } }
                              ],
                              "overlay": {
                                "musicItemThumbnailOverlayRenderer": {
                                  "content": {
                                    "musicPlayButtonRenderer": {
                                      "playNavigationEndpoint": {
                                        "watchEndpoint": { "videoId": "ano_bando_vid_01" }
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
        }
        """.trimIndent()

        val data = client.parseRelatedResponse(fixture)
        assertEquals("About the Artist", data.aboutArtistHeader)
        assertEquals("Secondary column bio description.", data.aboutArtistDescription)
        assertEquals(1, data.sections.size)
        assertEquals("Similar Tracks", data.sections[0].title)
        assertEquals(HomeSectionStyle.QUICK_PICKS_GRID, data.sections[0].style)
        assertEquals("ano_bando_vid_01", data.sections[0].items[0].id)
    }

    @Test
    fun testParseRelatedResponseTwoColumnBrowseResultsWithTabsFallback() {
        // When secondaryContents is empty or missing, it falls back to twoColumn.tabs
        val fixture = """
        {
          "contents": {
            "twoColumnBrowseResultsRenderer": {
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
                                  "title": { "runs": [{ "text": "Fallback Tabs Carousel" }] }
                                }
                              },
                              "contents": [
                                {
                                  "musicResponsiveListItemRenderer": {
                                    "flexColumns": [
                                      { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "Karakara" }] } } },
                                      { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [{ "text": "Kessoku Band" }] } } }
                                    ],
                                    "overlay": {
                                      "musicItemThumbnailOverlayRenderer": {
                                        "content": {
                                          "musicPlayButtonRenderer": {
                                            "playNavigationEndpoint": {
                                              "watchEndpoint": { "videoId": "karakara_vid_02" }
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

        val data = client.parseRelatedResponse(fixture)
        assertEquals(1, data.sections.size)
        assertEquals("Fallback Tabs Carousel", data.sections[0].title)
        assertEquals("karakara_vid_02", data.sections[0].items[0].id)
    }

    @Test
    fun testParseRelatedResponseSingleColumnMultipleTabsWithFirstEmpty() {
        // Tab 0 has empty content; Tab 1 contains the actual sectionListRenderer
        val fixture = """
        {
          "contents": {
            "singleColumnBrowseResultsRenderer": {
              "tabs": [
                {
                  "tabRenderer": {
                    "content": {}
                  }
                },
                {
                  "tabRenderer": {
                    "content": {
                      "sectionListRenderer": {
                        "contents": [
                          {
                            "musicDescriptionShelfRenderer": {
                              "header": { "runs": [{ "text": "Artista Bio Tab 1" }] },
                              "description": { "runs": [{ "text": "Recuperado da segunda tab." }] }
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

        val data = client.parseRelatedResponse(fixture)
        assertEquals("Artista Bio Tab 1", data.aboutArtistHeader)
        assertEquals("Recuperado da segunda tab.", data.aboutArtistDescription)
        assertTrue(data.sections.isEmpty())
    }

    // =========================================================================
    // 4. ANIME BAND BIO FALLBACK WHEN CAROUSELS ARE EMPTY
    // =========================================================================

    @Test
    fun testAnimeBandBioFallbackWithEmptyCarousels() {
        // Niche anime band response having only a descriptionShelfRenderer and no carousel items.
        // This validates the condition in PlayerScreen:
        // `relatedData == null || (relatedData.sections.isEmpty() && relatedData.aboutArtistDescription.isNullOrBlank())`
        // which renders the bio card even when sections are empty!
        val fixture = """
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
                            "musicDescriptionShelfRenderer": {
                              "header": { "runs": [{ "text": "Sobre Kessoku Band" }] },
                              "description": { "runs": [{ "text": "Banda fictícia da franquia Bocchi the Rock! composta por Hitori Gotoh, Nijika Ijichi, Ryo Yamada e Ikuyo Kita." }] }
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

        val data = client.parseRelatedResponse(fixture)
        assertNotNull(data)
        assertEquals("Sobre Kessoku Band", data.aboutArtistHeader)
        assertTrue(data.aboutArtistDescription!!.contains("Bocchi the Rock!"))
        assertTrue("As seções de carrossel devem estar vazias", data.sections.isEmpty())
        assertFalse(
            "A biografia NÃO deve ser nula/em branco, permitindo que a UI exiba o card de bio mesmo sem carrosséis",
            data.aboutArtistDescription.isNullOrBlank()
        )
    }

    // =========================================================================
    // 5. TWO-ROW ITEM RENDERER PARSING INSIDE RELATED CAROUSELS
    // =========================================================================

    @Test
    fun testParseRelatedResponseWithTwoRowItemRenderer() {
        val fixture = """
        {
          "contents": {
            "sectionListRenderer": {
              "contents": [
                {
                  "musicCarouselShelfRenderer": {
                    "numItemsPerColumn": "1",
                    "header": {
                      "musicCarouselShelfBasicHeaderRenderer": {
                        "title": { "runs": [{ "text": "Álbuns Semelhantes" }] }
                      }
                    },
                    "contents": [
                      {
                        "musicTwoRowItemRenderer": {
                          "title": { "runs": [{ "text": "Bocchi the Rock! OST" }] },
                          "subtitle": { "runs": [{ "text": "Álbum • 2022" }] },
                          "navigationEndpoint": {
                            "watchEndpoint": { "videoId": "ost_track_vid_1" }
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
        """.trimIndent()

        val data = client.parseRelatedResponse(fixture)
        assertEquals(1, data.sections.size)
        assertEquals(HomeSectionStyle.TWO_ROW_CAROUSEL, data.sections[0].style)
        assertEquals(1, data.sections[0].items.size)
        assertEquals("Bocchi the Rock! OST", data.sections[0].items[0].title)
        assertEquals("ost_track_vid_1", data.sections[0].items[0].id)
    }
}
