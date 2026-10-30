package com.nerf.music.ui.viewmodel

import com.nerf.music.data.db.LibraryTrackEntity
import java.text.Normalizer
import java.util.Locale

internal data class RecapArtistCredit(val primary: List<String>, val featured: List<String>)

internal data class RecapArtistContribution(
    val name: String,
    val track: LibraryTrackEntity,
    val weight: Double
)

private val recapFeatureMarker = Regex("\\b(?:featuring|feat\\.?|ft\\.?)\\s+", RegexOption.IGNORE_CASE)
private val recapCreditSeparator = Regex("\\s*(?:,|;|/|&|×)\\s*|\\s+\\b(?:and|e|y|x)\\b\\s+", RegexOption.IGNORE_CASE)

/** Splits a display credit into contributors, giving explicit feat/ft credits a guest role. */
internal fun splitRecapArtistCredit(raw: String): RecapArtistCredit {
    val credit = raw.trim()
    if (credit.isBlank()) return RecapArtistCredit(emptyList(), emptyList())
    val marker = recapFeatureMarker.find(credit)
    val primaryText = marker?.let { credit.substring(0, it.range.first) } ?: credit
    val featuredText = marker?.let { credit.substring(it.range.last + 1) }
    val primaryNames = splitNames(primaryText)
    val featuredNames = featuredText?.let(::splitNames).orEmpty()
    // Many catalogues omit “feat.” and join collaborators directly. Keep the first
    // credited performer primary and treat the remaining names as collaborators.
    val primary = if (marker == null && primaryNames.size > 1) primaryNames.take(1) else primaryNames
    val featured = (if (marker == null) primaryNames.drop(1) else emptyList()) + featuredNames
    return RecapArtistCredit(
        primary = primary.distinctBy(::normalizeRecapArtistName),
        featured = featured.distinctBy(::normalizeRecapArtistName)
            .filterNot { guest -> primary.any { normalizeRecapArtistName(it) == normalizeRecapArtistName(guest) } }
    )
}

internal fun normalizeRecapArtistName(name: String): String = Normalizer.normalize(name, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .trim()
    .lowercase(Locale.ROOT)

private fun splitNames(credit: String): List<String> = credit.split(recapCreditSeparator)
    .map { it.trim().trim(',', ';', '/', '&', '×') }
    .filter(String::isNotBlank)
