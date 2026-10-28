package com.nerf.music.player

import android.os.Build
import android.util.Log
import com.nerf.music.data.model.LyricsData
import com.nerf.music.data.model.SyncedLyricsLine
import com.nerf.music.data.model.SyncedLyricsWord
import com.nerf.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Estado instantâneo de sincronização de Karaokê para o tempo atual do player.
 */
data class KaraokeSyncSnapshot(
    val activeLineIndex: Int = -1,
    val activeWordIndex: Int = -1,
    val lineProgress: Float = 0f,
    val wordProgress: Float = 0f,
    val isInstrumentalCountdown: Boolean = false,
    val countdownDots: Int = 0 // 1..3 pontos antes da entrada vocal (estilo Apple Music Sing)
)

data class TranslationLanguageOption(
    val code: String,
    val label: String,
    val shortBadge: String
)

/**
 * Motor de sincronização LRC em tempo real (Karaokê), Tradução de Letras (Lyrics Translation)
 * e Guia de Pronúncia Fonética (Show Pronunciation).
 */
object KaraokeLyricsEngine {
    private const val TAG = "KaraokeLyricsEngine"

    val SUPPORTED_TRANSLATION_LANGUAGES: List<TranslationLanguageOption> = listOf(
        TranslationLanguageOption("auto", "Automático (Sistema)", "AUTO"),
        TranslationLanguageOption("pt", "Português", "PT"),
        TranslationLanguageOption("en", "English", "EN"),
        TranslationLanguageOption("es", "Español", "ES"),
        TranslationLanguageOption("fr", "Français", "FR"),
        TranslationLanguageOption("ja", "日本語", "JA"),
        TranslationLanguageOption("ko", "한국어", "KO"),
        TranslationLanguageOption("de", "Deutsch", "DE"),
        TranslationLanguageOption("it", "Italiano", "IT")
    )

    private val lineTimestampPattern = Pattern.compile("""\[(\d{1,3}):(\d{2})(?:[\.:](\d{1,3}))?\]""")
    private val wordTimestampPattern = Pattern.compile("""<(\d{1,3}):(\d{2})(?:[\.:](\d{1,3}))?>([^<]*)""")
    private val offsetTagPattern = Pattern.compile("""^\[offset:\s*([+-]?\d+)\s*\]$""", Pattern.CASE_INSENSITIVE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val enrichedLyricsCache = ConcurrentHashMap<String, LyricsData>()

    /**
     * Converte letras estáticas (ex: fallback do YouTube Music / Musixmatch sem timestamps)
     * em linhas estruturadas com pronúncia fonética local imediata.
     */
    fun buildPlainLinesWithPronunciation(plainLyrics: String?): List<SyncedLyricsLine> {
        if (plainLyrics.isNullOrBlank() || plainLyrics.trim().equals("null", ignoreCase = true)) {
            return emptyList()
        }
        return plainLyrics
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            .mapIndexed { idx, lineText ->
                SyncedLyricsLine(
                    timeMs = idx * 3_000L,
                    text = lineText,
                    endTimeMs = (idx + 1) * 3_000L,
                    words = emptyList(),
                    pronunciationText = generateLocalPronunciation(lineText)
                )
            }
    }

    /**
     * Faz o parse completo de um arquivo/texto LRC suportando:
     * - Múltiplos timestamps na mesma linha: `[00:12.00][00:34.00]Refrão`
     * - Tag global `[offset:+/-ms]`
     * - Enhanced LRC word-by-word: `<00:12.10>Hello <00:12.50>world`
     * - Interpolação automática de palavras (Karaoke Word Wipe) quando o LRC possui apenas timestamps de linha.
     * - Geração imediata da guia de pronúncia fonética (`pronunciationText`).
     */
    fun parseLrcWithKaraokeTiming(lrcText: String): List<SyncedLyricsLine> {
        var globalOffsetMs = 0L
        val rawEntries = mutableListOf<Pair<Long, String>>()

        lrcText.lines().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) return@forEach

            val offsetMatcher = offsetTagPattern.matcher(trimmed)
            if (offsetMatcher.matches()) {
                globalOffsetMs = offsetMatcher.group(1)?.toLongOrNull() ?: 0L
                return@forEach
            }

            val timestamps = mutableListOf<Long>()
            val matcher = lineTimestampPattern.matcher(trimmed)
            var lastMatchEnd = 0
            while (matcher.find()) {
                if (matcher.start() == lastMatchEnd || timestamps.isNotEmpty()) {
                    val min = matcher.group(1)?.toLongOrNull() ?: 0L
                    val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                    val fracStr = matcher.group(3) ?: "0"
                    val ms = when (fracStr.length) {
                        1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                        2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                        else -> fracStr.take(3).toLongOrNull() ?: 0L
                    }
                    val totalMs = ((min * 60_000L) + (sec * 1_000L) + ms - globalOffsetMs).coerceAtLeast(0L)
                    timestamps.add(totalMs)
                    lastMatchEnd = matcher.end()
                } else {
                    break
                }
            }

            if (timestamps.isNotEmpty() && lastMatchEnd <= trimmed.length) {
                val content = trimmed.substring(lastMatchEnd).trim()
                if (content.isNotBlank() && !content.equals("null", ignoreCase = true)) {
                    for (ts in timestamps) {
                        rawEntries.add(ts to content)
                    }
                }
            }
        }

        val sorted = rawEntries.sortedBy { it.first }
        if (sorted.isEmpty()) return emptyList()

        return sorted.mapIndexed { index, (startTimeMs, rawContent) ->
            val nextLineStartMs = sorted.getOrNull(index + 1)?.first
            val maxNaturalDurationMs = 5_500L
            val estimatedEndMs = if (nextLineStartMs != null && nextLineStartMs > startTimeMs) {
                minOf(nextLineStartMs, startTimeMs + maxNaturalDurationMs)
            } else {
                startTimeMs + 4_000L
            }

            val (cleanText, words) = extractOrSynthesizeWords(
                rawContent = rawContent,
                lineStartMs = startTimeMs,
                lineEndMs = estimatedEndMs
            )

            val pronunciation = generateLocalPronunciation(cleanText)

            SyncedLyricsLine(
                timeMs = startTimeMs,
                text = cleanText,
                endTimeMs = estimatedEndMs,
                words = words,
                pronunciationText = pronunciation
            )
        }.filter { it.text.isNotBlank() }
    }

    private fun extractOrSynthesizeWords(
        rawContent: String,
        lineStartMs: Long,
        lineEndMs: Long
    ): Pair<String, List<SyncedLyricsWord>> {
        val wordMatcher = wordTimestampPattern.matcher(rawContent)
        val explicitWords = mutableListOf<Pair<Long, String>>()
        while (wordMatcher.find()) {
            val min = wordMatcher.group(1)?.toLongOrNull() ?: 0L
            val sec = wordMatcher.group(2)?.toLongOrNull() ?: 0L
            val fracStr = wordMatcher.group(3) ?: "0"
            val ms = when (fracStr.length) {
                1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                else -> fracStr.take(3).toLongOrNull() ?: 0L
            }
            val ts = (min * 60_000L) + (sec * 1_000L) + ms
            val token = wordMatcher.group(4)?.trim().orEmpty()
            if (token.isNotEmpty()) {
                explicitWords.add(ts to token)
            }
        }

        if (explicitWords.isNotEmpty()) {
            val cleanText = explicitWords.joinToString(" ") { it.second }
            val timedWords = explicitWords.mapIndexed { idx, (wStart, wText) ->
                val wEnd = explicitWords.getOrNull(idx + 1)?.first?.coerceAtLeast(wStart + 80L)
                    ?: lineEndMs.coerceAtLeast(wStart + 250L)
                SyncedLyricsWord(
                    startTimeMs = wStart,
                    endTimeMs = wEnd,
                    text = wText
                )
            }
            return cleanText to timedWords
        }

        // Remove eventuais tags residuais e divide em palavras para interpolação silábica de Karaokê
        val cleanText = rawContent.replace(Regex("""<[^>]+>"""), "").trim().replace(Regex("""\s+"""), " ")
        val tokens = cleanText.split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return cleanText to emptyList()

        val totalDuration = (lineEndMs - lineStartMs).coerceAtLeast(300L)
        // Usa 88% da janela da linha para cantar as palavras e deixa 12% de respiro final no último termo
        val activeSingDuration = (totalDuration * 0.88).toLong().coerceAtLeast(250L)
        val weights = tokens.map { token ->
            // Palavras maiores e com pontuação final têm peso ligeiramente maior na métrica vocal
            val baseLen = token.codePointCount(0, token.length).coerceAtLeast(1)
            val punctuationPause = if (token.endsWith(",") || token.endsWith(";") || token.endsWith(".") || token.endsWith("!") || token.endsWith("?")) 2 else 0
            (baseLen + punctuationPause).toLong()
        }
        val totalWeight = weights.sum().coerceAtLeast(1L)

        var accumulatedWeight = 0L
        val synthesizedWords = tokens.mapIndexed { idx, token ->
            val wordStart = lineStartMs + (activeSingDuration * accumulatedWeight) / totalWeight
            accumulatedWeight += weights[idx]
            val wordEnd = if (idx == tokens.lastIndex) {
                (lineStartMs + activeSingDuration).coerceAtLeast(wordStart + 80L)
            } else {
                (lineStartMs + (activeSingDuration * accumulatedWeight) / totalWeight).coerceAtLeast(wordStart + 60L)
            }
            SyncedLyricsWord(
                startTimeMs = wordStart,
                endTimeMs = wordEnd,
                text = token
            )
        }

        return cleanText to synthesizedWords
    }

    /**
     * Resolve em O(log N) o estado de sincronização de Karaokê para o [rawPositionMs] do player,
     * aplicando o [offsetMs] configurável pelo usuário.
     */
    fun resolveSyncSnapshot(
        lyrics: LyricsData?,
        rawPositionMs: Long,
        offsetMs: Long = 0L
    ): KaraokeSyncSnapshot {
        if (lyrics == null || !lyrics.isSynced || lyrics.syncedLines.isEmpty()) {
            return KaraokeSyncSnapshot()
        }

        val lines = lyrics.syncedLines
        val adjustedPosMs = (rawPositionMs + offsetMs).coerceAtLeast(0L)

        // Busca binária para encontrar a última linha cujo timeMs <= adjustedPosMs
        var low = 0
        var high = lines.lastIndex
        var activeIdx = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timeMs <= adjustedPosMs) {
                activeIdx = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        // Verifica se estamos em uma introdução ou interlúdio instrumental antes da próxima linha
        val nextLineIdx = activeIdx + 1
        val nextLine = lines.getOrNull(nextLineIdx)
        val currentLine = lines.getOrNull(activeIdx)

        val isBeforeFirstLine = activeIdx == -1 && nextLine != null && nextLine.timeMs >= 2_500L
        val isLongInterlude = currentLine != null && nextLine != null &&
                (nextLine.timeMs - currentLine.endTimeMs) >= 4_000L &&
                adjustedPosMs >= currentLine.endTimeMs

        var isCountdown = false
        var countdownDots = 0
        if ((isBeforeFirstLine || isLongInterlude) && nextLine != null) {
            val timeUntilNextMs = nextLine.timeMs - adjustedPosMs
            if (timeUntilNextMs in 100L..3_600L) {
                isCountdown = true
                countdownDots = when {
                    timeUntilNextMs > 2_400L -> 3
                    timeUntilNextMs > 1_200L -> 2
                    else -> 1
                }
            }
        }

        if (currentLine == null) {
            return KaraokeSyncSnapshot(
                activeLineIndex = -1,
                isInstrumentalCountdown = isCountdown,
                countdownDots = countdownDots
            )
        }

        val effectiveEndMs = if (currentLine.endTimeMs > currentLine.timeMs) {
            currentLine.endTimeMs
        } else {
            nextLine?.timeMs ?: (currentLine.timeMs + 4_000L)
        }

        val lineDuration = (effectiveEndMs - currentLine.timeMs).coerceAtLeast(1L)
        val lineProgress = ((adjustedPosMs - currentLine.timeMs).toFloat() / lineDuration.toFloat()).coerceIn(0f, 1f)

        var activeWordIdx = -1
        var wordProgress = 0f
        val words = currentLine.words
        if (words.isNotEmpty()) {
            activeWordIdx = words.indexOfLast { adjustedPosMs >= it.startTimeMs }
            if (activeWordIdx >= 0) {
                val word = words[activeWordIdx]
                val wDur = (word.endTimeMs - word.startTimeMs).coerceAtLeast(1L)
                wordProgress = ((adjustedPosMs - word.startTimeMs).toFloat() / wDur.toFloat()).coerceIn(0f, 1f)
            }
        }

        return KaraokeSyncSnapshot(
            activeLineIndex = activeIdx,
            activeWordIndex = activeWordIdx,
            lineProgress = lineProgress,
            wordProgress = wordProgress,
            isInstrumentalCountdown = isCountdown,
            countdownDots = countdownDots
        )
    }

    /**
     * Enriquece [LyricsData] com Tradução linha-a-linha (Lyrics Translation) e
     * Pronúncia fonética/romanizada (Show Pronunciation), funcionando tanto para
     * letras sincronizadas (LRCLIB) quanto para letras estáticas de fallback (YouTube Music).
     */
    suspend fun enrichLyricsWithTranslationAndPronunciation(
        lyrics: LyricsData,
        preferredTargetLang: String = "auto"
    ): LyricsData = withContext(Dispatchers.IO) {
        val resolvedLangCode = if (preferredTargetLang.isBlank() || preferredTargetLang.equals("auto", ignoreCase = true)) {
            Locale.getDefault().language.ifBlank { "pt" }
        } else {
            preferredTargetLang.lowercase(Locale.ROOT)
        }
        val isAutoMode = preferredTargetLang.isBlank() || preferredTargetLang.equals("auto", ignoreCase = true)
        val cacheKey = "${lyrics.trackId}|${preferredTargetLang.lowercase(Locale.ROOT)}"
        enrichedLyricsCache[cacheKey]?.let { return@withContext it }

        // 1. Garante que todas as linhas sincronizadas já tenham palavras de Karaokê e pronúncia base local
        val baseSyncedLines = lyrics.syncedLines.mapIndexed { idx, line ->
            val nextTime = lyrics.syncedLines.getOrNull(idx + 1)?.timeMs
            val endMs = if (line.endTimeMs > line.timeMs) {
                line.endTimeMs
            } else if (nextTime != null && nextTime > line.timeMs) {
                minOf(nextTime, line.timeMs + 5_500L)
            } else {
                line.timeMs + 4_000L
            }
            val (_, words) = if (line.words.isNotEmpty()) {
                line.text to line.words
            } else {
                extractOrSynthesizeWords(line.text, line.timeMs, endMs)
            }
            val pron = line.pronunciationText ?: generateLocalPronunciation(line.text)
            line.copy(
                endTimeMs = endMs,
                words = words,
                pronunciationText = pron
            )
        }

        // 1b. Garante linhas estruturadas para letras estáticas (fallback do YouTube Music / Musixmatch)
        val basePlainLines = if (lyrics.plainLines.isNotEmpty()) {
            lyrics.plainLines.map { line ->
                line.copy(pronunciationText = line.pronunciationText ?: generateLocalPronunciation(line.text))
            }
        } else {
            buildPlainLinesWithPronunciation(lyrics.plainLyrics)
        }

        if (baseSyncedLines.isEmpty() && basePlainLines.isEmpty()) {
            return@withContext lyrics
        }

        val activeLinesToEnrich = if (baseSyncedLines.isNotEmpty()) baseSyncedLines else basePlainLines

        // 2. Tradução em lote preservando alinhamento 1:1 das linhas
        try {
            val sourceTexts = activeLinesToEnrich.map { it.text }
            if (sourceTexts.isEmpty()) return@withContext lyrics

            val sampleText = sourceTexts.take(8).joinToString(" ")
            val isProbablyPortuguese = looksLikePortuguese(sampleText)
            val targetLang = if (isAutoMode && resolvedLangCode.startsWith("pt", ignoreCase = true) && isProbablyPortuguese) {
                "en" // Em modo automático, se a música já está em Português, traduz para Inglês
            } else {
                resolvedLangCode
            }

            val batchResult = fetchBatchTranslationAndRomanization(sourceTexts, targetLang)
            val updatedLines = activeLinesToEnrich.mapIndexed { idx, line ->
                val remoteTranslation = batchResult.translations.getOrNull(idx)?.takeIf {
                    it.isNotBlank() && !it.equals(line.text, ignoreCase = true)
                }
                val remoteRomanization = batchResult.romanizations.getOrNull(idx)?.takeIf { it.isNotBlank() }
                val finalPronunciation = remoteRomanization ?: line.pronunciationText ?: generateLocalPronunciation(line.text)

                line.copy(
                    translatedText = remoteTranslation ?: line.translatedText,
                    pronunciationText = finalPronunciation
                )
            }

            val enriched = if (baseSyncedLines.isNotEmpty()) {
                lyrics.copy(
                    syncedLines = updatedLines,
                    plainLines = basePlainLines,
                    detectedLanguage = batchResult.detectedSourceLang,
                    translatedLanguage = targetLang,
                    hasPronunciation = updatedLines.any { !it.pronunciationText.isNullOrBlank() }
                )
            } else {
                lyrics.copy(
                    plainLines = updatedLines,
                    detectedLanguage = batchResult.detectedSourceLang,
                    translatedLanguage = targetLang,
                    hasPronunciation = updatedLines.any { !it.pronunciationText.isNullOrBlank() }
                )
            }

            if (updatedLines.any { !it.translatedText.isNullOrBlank() }) {
                enrichedLyricsCache[cacheKey] = enriched
            }
            enriched
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao traduzir letras remotamente, mantendo pronúncia local: ${e.message}")
            lyrics.copy(
                syncedLines = baseSyncedLines,
                plainLines = basePlainLines,
                hasPronunciation = activeLinesToEnrich.any { !it.pronunciationText.isNullOrBlank() }
            )
        }
    }

    private data class BatchTranslationResult(
        val translations: List<String>,
        val romanizations: List<String?>,
        val detectedSourceLang: String?
    )

    private fun fetchBatchTranslationAndRomanization(
        lines: List<String>,
        targetLang: String
    ): BatchTranslationResult {
        val allTranslations = MutableList(lines.size) { "" }
        val allRomanizations = MutableList<String?>(lines.size) { null }
        var detectedLang: String? = null

        // Divide em blocos de até 28 linhas para evitar URLs muito longas e manter correspondência exata por índice
        val chunkSize = 28
        val separator = "\n"

        for (startIdx in lines.indices step chunkSize) {
            val endIdx = minOf(startIdx + chunkSize, lines.size)
            val chunk = lines.subList(startIdx, endIdx)
            val joined = chunk.joinToString(separator)
            val encoded = URLEncoder.encode(joined, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&dt=rm&q=$encoded"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile)")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use
                val body = response.body?.string() ?: return@use
                val rootArray = JSONArray(body)
                if (detectedLang == null && rootArray.length() > 2 && !rootArray.isNull(2)) {
                    detectedLang = rootArray.optString(2)
                }

                val sentencesArray = rootArray.optJSONArray(0) ?: return@use
                val translatedBuilder = StringBuilder()
                val romanizedBuilder = StringBuilder()

                for (i in 0 until sentencesArray.length()) {
                    val item = sentencesArray.optJSONArray(i) ?: continue
                    val transPart = if (!item.isNull(0)) item.optString(0, "") else ""
                    if (transPart.isNotEmpty()) {
                        translatedBuilder.append(transPart)
                    }
                    // O índice 3 no último elemento ou em cada sentença com dt=rm traz a romanização fonética oficial (Pinyin, Romaji, Revised Korean, etc.)
                    val romPart = if (item.length() > 3 && !item.isNull(3)) item.optString(3, "") else ""
                    if (romPart.isNotEmpty()) {
                        if (romanizedBuilder.isNotEmpty()) romanizedBuilder.append("\n")
                        romanizedBuilder.append(romPart)
                    }
                }

                val translatedChunkLines = translatedBuilder.toString().split("\n")
                for (offset in chunk.indices) {
                    val globalIndex = startIdx + offset
                    allTranslations[globalIndex] = translatedChunkLines.getOrNull(offset)?.trim().orEmpty()
                }

                if (romanizedBuilder.isNotEmpty()) {
                    val romLines = romanizedBuilder.toString().split("\n")
                    if (romLines.size == chunk.size) {
                        for (offset in chunk.indices) {
                            allRomanizations[startIdx + offset] = romLines[offset].trim()
                        }
                    }
                }
            }
        }

        return BatchTranslationResult(
            translations = allTranslations,
            romanizations = allRomanizations,
            detectedSourceLang = detectedLang
        )
    }

    /**
     * Gera a guia de pronúncia fonética local instantânea (0ms):
     * 1. Se o texto contiver caracteres não-latinos (Hangul, Kanji/Kana, Hanzi, Cirílico, Grego, Árabe, etc.),
     *    translitera para o alfabeto latino usando ICU4J nativo do Android (API 29+) ou tabela Hangul/Kana.
     * 2. Se o texto já estiver no alfabeto latino (ex: Inglês/Espanhol/Francês), gera uma transcrição
     *    fonética intuitiva para cantar junto (Sing-Along Phonetics).
     */
    fun generateLocalPronunciation(text: String): String? {
        val clean = text.trim()
        if (clean.isEmpty()) return null

        val hasNonLatinScript = clean.any { ch ->
            val block = Character.UnicodeBlock.of(ch)
            block == Character.UnicodeBlock.HANGUL_SYLLABLES ||
                    block == Character.UnicodeBlock.HANGUL_JAMO ||
                    block == Character.UnicodeBlock.HIRAGANA ||
                    block == Character.UnicodeBlock.KATAKANA ||
                    block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                    block == Character.UnicodeBlock.CYRILLIC ||
                    block == Character.UnicodeBlock.GREEK ||
                    block == Character.UnicodeBlock.ARABIC ||
                    block == Character.UnicodeBlock.HEBREW ||
                    block == Character.UnicodeBlock.THAI ||
                    block == Character.UnicodeBlock.DEVANAGARI
        }

        if (hasNonLatinScript) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val transliterator = android.icu.text.Transliterator.getInstance("Any-Latin; Latin-ASCII")
                    val romanized = transliterator.transliterate(clean).trim()
                    if (romanized.isNotBlank() && !romanized.equals(clean, ignoreCase = true)) {
                        return romanized
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "ICU Transliterator fallback: ${e.message}")
                }
            }
            val hangulFallback = romanizeHangulFallback(clean)
            if (!hangulFallback.equals(clean, ignoreCase = true)) {
                return hangulFallback
            }
        }

        // Para letras em alfabeto latino (ex: Inglês), aplica guia fonética cantável
        return buildLatinSingAlongPronunciation(clean)
    }

    private fun romanizeHangulFallback(input: String): String {
        val initials = arrayOf("g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h")
        val medials = arrayOf("a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa", "wae", "oe", "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i")
        val finals = arrayOf("", "k", "k", "k", "n", "n", "n", "t", "l", "k", "m", "l", "l", "l", "p", "l", "m", "p", "p", "t", "t", "ng", "t", "t", "k", "t", "p", "t")

        val sb = StringBuilder()
        for (ch in input) {
            val code = ch.code
            if (code in 0xAC00..0xD7A3) {
                val syllableIndex = code - 0xAC00
                val initialIdx = syllableIndex / (21 * 28)
                val medialIdx = (syllableIndex % (21 * 28)) / 28
                val finalIdx = syllableIndex % 28
                sb.append(initials[initialIdx])
                sb.append(medials[medialIdx])
                sb.append(finals[finalIdx])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private val commonPhoneticWords = mapOf(
        "i" to "ai",
        "i'm" to "aim",
        "i'll" to "aiu",
        "i've" to "aiv",
        "i'd" to "aid",
        "you" to "iú",
        "your" to "iór",
        "you're" to "iór",
        "we" to "uí",
        "we're" to "uír",
        "they" to "dei",
        "the" to "dâ",
        "this" to "dís",
        "that" to "dét",
        "these" to "díz",
        "those" to "dôuz",
        "there" to "dér",
        "their" to "dér",
        "where" to "uér",
        "what" to "uót",
        "when" to "uén",
        "why" to "uai",
        "who" to "rú",
        "how" to "ráu",
        "with" to "uíd",
        "without" to "uidáut",
        "through" to "trú",
        "though" to "dôu",
        "thought" to "tót",
        "love" to "lâv",
        "heart" to "rárt",
        "night" to "nait",
        "light" to "lait",
        "right" to "rait",
        "bright" to "brait",
        "fight" to "fait",
        "sight" to "sait",
        "might" to "mait",
        "high" to "rái",
        "sigh" to "sái",
        "know" to "nôu",
        "now" to "náu",
        "down" to "dáun",
        "town" to "táun",
        "around" to "arráund",
        "sound" to "sáund",
        "found" to "fáund",
        "world" to "uôrld",
        "girl" to "gârl",
        "time" to "taim",
        "life" to "laif",
        "fire" to "faier",
        "desire" to "dizáier",
        "eyes" to "aiz",
        "sky" to "skai",
        "fly" to "flai",
        "cry" to "crai",
        "try" to "trai",
        "my" to "mai",
        "by" to "bai",
        "never" to "néver",
        "ever" to "éver",
        "forever" to "foréver",
        "together" to "tuguéder",
        "nothing" to "nâthin",
        "something" to "sâmthin",
        "everything" to "évri-thin",
        "anything" to "êni-thin",
        "one" to "uân",
        "once" to "uâns",
        "only" to "ônli",
        "come" to "câm",
        "some" to "sâm",
        "done" to "dân",
        "gone" to "gón",
        "want" to "uónt",
        "don't" to "dônt",
        "can't" to "ként",
        "won't" to "uônt",
        "didn't" to "dídent",
        "doesn't" to "dâzent",
        "isn't" to "ízent",
        "wasn't" to "uózent",
        "could" to "cúd",
        "would" to "uúd",
        "should" to "shúd",
        "good" to "gúd",
        "look" to "lúk",
        "take" to "teik",
        "make" to "meik",
        "wake" to "ueik",
        "break" to "breik",
        "shake" to "sheik",
        "away" to "auêi",
        "stay" to "stei",
        "play" to "plei",
        "say" to "sei",
        "day" to "dei",
        "way" to "uei",
        "feel" to "fíl",
        "real" to "ríl",
        "dream" to "drím",
        "dreams" to "drímz",
        "sweet" to "suít",
        "keep" to "kíp",
        "deep" to "díp",
        "sleep" to "slíp",
        "need" to "níd",
        "see" to "sí",
        "free" to "frí",
        "me" to "mí",
        "be" to "bí",
        "he" to "rí",
        "she" to "shí",
        "all" to "ól",
        "call" to "cól",
        "fall" to "fól",
        "wall" to "uól",
        "small" to "smól",
        "walk" to "uók",
        "talk" to "tók",
        "cause" to "cóz",
        "because" to "bicóz",
        "always" to "óluêiz",
        "again" to "aguén",
        "friend" to "frénd",
        "friends" to "fréndz",
        "baby" to "bêibi",
        "crazy" to "crêizi",
        "lady" to "lêidi",
        "maybe" to "mêibi",
        "music" to "miúzik",
        "dance" to "déns",
        "chance" to "tchéns",
        "change" to "tchêindj",
        "changes" to "tchêindjez",
        "stranger" to "strêindjer",
        "danger" to "dêindjer",
        "soul" to "sôul",
        "hold" to "rôuld",
        "cold" to "côuld",
        "gold" to "gôuld",
        "told" to "tôuld",
        "old" to "ôuld",
        "home" to "rôum",
        "alone" to "alôun",
        "phone" to "fôun",
        "stone" to "stôun",
        "mind" to "maind",
        "find" to "faind",
        "blind" to "blaind",
        "behind" to "birráind",
        "kind" to "kaind",
        "wind" to "uínd",
        "shine" to "shain",
        "mine" to "main",
        "fine" to "fain",
        "line" to "lain",
        "wine" to "uain",
        "true" to "trú",
        "blue" to "blú",
        "new" to "niú",
        "knew" to "niú",
        "few" to "fiú",
        "yeah" to "ié",
        "oh" to "ôu",
        "no" to "nôu",
        "go" to "gôu",
        "so" to "sôu",
        "slow" to "slôu",
        "glow" to "glôu",
        "flow" to "flôu",
        "show" to "shôu",
        "grow" to "grôu",
        "throw" to "trôu"
    )

    private fun buildLatinSingAlongPronunciation(line: String): String {
        if (looksLikePortuguese(line)) {
            // Para Português, separa sílabas tônicas/ritmo leve ou mantém limpo
            return line.lowercase(Locale.ROOT)
        }

        val words = line.split(Regex("""\s+"""))
        val transformed = words.map { rawWord ->
            val prefix = rawWord.takeWhile { !it.isLetterOrDigit() && it != '\'' }
            val suffix = rawWord.takeLastWhile { !it.isLetterOrDigit() && it != '\'' }
            val core = rawWord.substring(prefix.length, (rawWord.length - suffix.length).coerceAtLeast(prefix.length))
            if (core.isEmpty()) return@map rawWord

            val lower = core.lowercase(Locale.ROOT)
            val direct = commonPhoneticWords[lower]
            if (direct != null) {
                return@map "$prefix$direct$suffix"
            }

            // Regras fonéticas de aproximação para cantar junto
            var w = lower
            w = w.replace("tion", "shân")
            w = w.replace("sion", "jân")
            w = w.replace("igh", "ai")
            w = w.replace("ought", "ót")
            w = w.replace("aught", "ót")
            w = w.replace("ough", "ôu")
            w = w.replace("eigh", "êi")
            w = w.replace("tch", "tch")
            w = w.replace("ch", "tch")
            w = w.replace("sh", "sh")
            w = w.replace("th", "d")
            w = w.replace("ph", "f")
            w = w.replace("wh", "u")
            w = w.replace("kn", "n")
            w = w.replace("wr", "r")
            w = w.replace("ck", "k")
            w = w.replace("qu", "ku")
            w = w.replace("oo", "ú")
            w = w.replace("ee", "í")
            w = w.replace("ea", "í")
            w = w.replace("ai", "êi")
            w = w.replace("ay", "êi")
            w = w.replace("ey", "êi")
            w = w.replace("oa", "ôu")
            w = w.replace("ow", "ôu")
            w = w.replace("ou", "áu")
            w = w.replace("au", "ó")
            w = w.replace("aw", "ó")
            w = w.replace("ew", "iú")
            w = w.replace("oy", "ói")
            w = w.replace("oi", "ói")
            if (w.endsWith("ing") && w.length > 4) {
                w = w.dropLast(3) + "in"
            } else if (w.endsWith("ed") && w.length > 4) {
                w = w.dropLast(2) + "d"
            } else if (w.endsWith("le") && w.length > 3) {
                w = w.dropLast(2) + "ôl"
            } else if (w.endsWith("e") && w.length > 3 && !w.endsWith("ee")) {
                w = w.dropLast(1)
            }
            if (w.startsWith("w")) {
                w = "u" + w.drop(1)
            } else if (w.startsWith("y") && w.length > 1) {
                w = "i" + w.drop(1)
            }
            "$prefix$w$suffix"
        }
        return transformed.joinToString(" ")
    }

    private fun looksLikePortuguese(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        if (lower.any { it in "ãõçêôáéíóúà" }) return true
        val ptTokens = setOf("não", "nao", "você", "voce", "coração", "amor", "então", "pra", "meu", "minha", "tudo", "mais", "quando", "porque")
        val tokens = lower.split(Regex("""\W+""")).filter { it.isNotBlank() }
        return tokens.count { it in ptTokens } >= 2
    }

    // =========================================================================
    // SELEÇÃO DE INSTRUMENTAL DE ALTA PRECISÃO (ANTI FALSO-POSITIVO)
    // =========================================================================

    private val instrumentalKeywords = listOf(
        "instrumental",
        "karaoke",
        "karaokê",
        "off vocal",
        "off-vocal",
        "backing track",
        "playback",
        "sem voz",
        "minus one"
    )

    private val forbiddenModifiers = listOf(
        "slowed",
        "reverb",
        "sped up",
        "speed up",
        "nightcore",
        "8d",
        "remix",
        "bootleg",
        "mashup",
        "lofi",
        "lo-fi",
        "8 bit",
        "8-bit",
        "piano cover",
        "guitar cover",
        "violin cover",
        "cello cover",
        "flute cover",
        "saxophone cover",
        "kalimba",
        "music box",
        "ringtone",
        "parodia",
        "parody"
    )

    fun normalizeForMatch(input: String): String {
        if (input.isBlank()) return ""
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return decomposed
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    fun extractCleanBaseTitle(rawTitle: String): String {
        return rawTitle
            .replace(Regex("""\s*\([^)]*(?:official|video|audio|lyric|clipe|cli?p|visualizer|remaster|hd|4k|feat|ft\.|prod\.)[^)]*\)\s*""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\s*\[[^\]]*(?:official|video|audio|lyric|clipe|cli?p|visualizer|remaster|hd|4k|feat|ft\.|prod\.)[^\]]*\]\s*""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\s*-\s*(?:official\s*(?:music\s*)?(?:video|audio|lyric\s*video)|clipe\s*oficial|áudio\s*oficial).*$""", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifBlank { rawTitle.trim() }
    }

    fun extractPrimaryArtist(rawArtist: String): String {
        return rawArtist
            .split(",", "&", "/", ";", "feat.", "ft.", "Feat.", "FT.", "part.", "Part.")
            .firstOrNull()
            ?.replace(Regex("""\s*-\s*Topic$""", RegexOption.IGNORE_CASE), "")
            ?.trim()
            .orEmpty()
    }

    fun buildInstrumentalSearchQueries(track: Track): List<String> {
        val baseTitle = extractCleanBaseTitle(track.title)
        val primaryArtist = extractPrimaryArtist(track.artist)
        val queries = mutableListOf<String>()
        if (primaryArtist.isNotBlank()) {
            queries.add("$baseTitle $primaryArtist instrumental")
            queries.add("$baseTitle $primaryArtist karaoke playback")
        } else {
            queries.add("$baseTitle official instrumental")
        }
        return queries.distinct()
    }

    fun selectBestInstrumentalCandidate(
        originalTrack: Track,
        candidates: List<Track>
    ): Track? {
        if (candidates.isEmpty()) return null

        val cleanBaseTitle = extractCleanBaseTitle(originalTrack.title)
        val normBaseTitle = normalizeForMatch(cleanBaseTitle)
        if (normBaseTitle.isBlank()) return null
        val baseTitleWords = normBaseTitle.split(" ").filter { it.isNotBlank() }

        val primaryArtist = extractPrimaryArtist(originalTrack.artist)
        val normPrimaryArtist = normalizeForMatch(primaryArtist)
        val artistWords = normPrimaryArtist.split(" ").filter { it.isNotBlank() }

        val normOrigFullTitle = normalizeForMatch(originalTrack.title)
        val origIsLive = normOrigFullTitle.contains("ao vivo") || containsWholeWord(normOrigFullTitle, "live")
        val origIsAcoustic = normOrigFullTitle.contains("acustico") || normOrigFullTitle.contains("acoustic")

        var bestCandidate: Track? = null
        var bestScore = -1

        for (cand in candidates) {
            if (cand.id.isBlank() || cand.id == originalTrack.id) continue

            val rawCandTitleLower = cand.title.lowercase(Locale.ROOT)
            val normCandTitle = normalizeForMatch(cand.title)
            val normCandArtist = normalizeForMatch(cand.artist)

            val hasInstKeyword = instrumentalKeywords.any { kw ->
                val normKw = normalizeForMatch(kw)
                rawCandTitleLower.contains(kw) || containsWholePhrase(normCandTitle, normKw)
            }
            if (!hasInstKeyword) continue

            val hasForbiddenModifier = forbiddenModifiers.any { mod ->
                val normMod = normalizeForMatch(mod)
                containsWholePhrase(normCandTitle, normMod) && !containsWholePhrase(normOrigFullTitle, normMod)
            }
            if (hasForbiddenModifier) continue

            val candIsLive = normCandTitle.contains("ao vivo") || containsWholeWord(normCandTitle, "live")
            if (candIsLive != origIsLive) continue

            val candIsAcoustic = normCandTitle.contains("acustico") || normCandTitle.contains("acoustic")
            if (candIsAcoustic != origIsAcoustic) continue

            val artistMatchesChannel = normPrimaryArtist.isNotBlank() && (
                containsWholePhrase(normCandArtist, normPrimaryArtist) ||
                    (normCandArtist.length >= 3 && containsWholePhrase(normPrimaryArtist, normCandArtist)) ||
                    (artistWords.size >= 2 && artistWords.all { containsWholeWord(normCandArtist, it) })
                )

            val artistMatchesInTitle = normPrimaryArtist.isNotBlank() && (
                containsWholePhrase(normCandTitle, normPrimaryArtist) ||
                    (artistWords.size >= 2 && artistWords.all { containsWholeWord(normCandTitle, it) })
                )

            if (normPrimaryArtist.isNotBlank() && !artistMatchesChannel && !artistMatchesInTitle) {
                continue
            }

            if (!containsWholePhrase(normCandTitle, normBaseTitle)) {
                val matchedWords = baseTitleWords.count { containsWholeWord(normCandTitle, it) }
                if (baseTitleWords.size < 2 || matchedWords < baseTitleWords.size) {
                    continue
                }
            }

            val strippedCandTitle = stripInstrumentalAndArtistTokens(normCandTitle, artistWords)
            val strippedWords = strippedCandTitle.split(" ").filter { it.isNotBlank() }

            val extraWordsCount = strippedWords.count { it !in baseTitleWords }
            if (baseTitleWords.size == 1 && extraWordsCount > 0) {
                continue
            }
            if (extraWordsCount > 2) {
                continue
            }

            val origDur = originalTrack.durationSeconds
            val candDur = cand.durationSeconds
            var durationScore = 0
            if (origDur > 0L && candDur > 0L) {
                val diffSec = kotlin.math.abs(candDur - origDur)
                if (diffSec > 5L) continue
                durationScore = when {
                    diffSec <= 1L -> 25
                    diffSec <= 2L -> 20
                    diffSec <= 3L -> 14
                    else -> 8
                }
            } else {
                if (!artistMatchesChannel || extraWordsCount > 0) continue
                durationScore = 5
            }

            var score = durationScore
            if (artistMatchesChannel) {
                score += 45
            } else if (artistMatchesInTitle) {
                score += 32
            }

            if (strippedCandTitle == normBaseTitle || extraWordsCount == 0) {
                score += 35
            } else if (extraWordsCount == 1) {
                score += 20
            }

            if (normCandTitle.contains("instrumental") || normCandTitle.contains("off vocal")) {
                score += 10
            }

            if (score >= 75 && score > bestScore) {
                bestScore = score
                bestCandidate = cand
            }
        }

        return bestCandidate
    }

    private fun containsWholeWord(haystack: String, word: String): Boolean {
        if (haystack.isBlank() || word.isBlank()) return false
        return Regex("""(?:^|\s)${Regex.escape(word)}(?:\s|$)""").containsMatchIn(haystack)
    }

    private fun containsWholePhrase(haystack: String, phrase: String): Boolean {
        if (haystack.isBlank() || phrase.isBlank()) return false
        return Regex("""(?:^|\s)${Regex.escape(phrase)}(?:\s|$)""").containsMatchIn(haystack)
    }

    private fun stripInstrumentalAndArtistTokens(normTitle: String, artistWords: List<String>): String {
        var working = " $normTitle "
        val removablePhrases = listOf(
            "official instrumental",
            "instrumental version",
            "versao instrumental",
            "official karaoke",
            "karaoke version",
            "versao karaoke",
            "originally performed by",
            "in the style of",
            "backing track",
            "off vocal",
            "minus one",
            "sem voz",
            "instrumental",
            "karaoke",
            "playback",
            "official",
            "oficial",
            "audio",
            "video",
            "version",
            "versao",
            "hd",
            "hq"
        )
        for (phrase in removablePhrases) {
            working = working.replace(Regex("""(?:^|\s)${Regex.escape(phrase)}(?:\s|$)"""), " ")
        }
        for (artistWord in artistWords) {
            if (artistWord.length >= 2) {
                working = working.replace(Regex("""(?:^|\s)${Regex.escape(artistWord)}(?:\s|$)"""), " ")
            }
        }
        return working.trim().replace(Regex("\\s+"), " ")
    }
}
