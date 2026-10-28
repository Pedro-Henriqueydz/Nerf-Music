package com.nerf.music.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Padrões de normalização de volume (Loudness Normalization) segundo normas da indústria e DSP.
 */
enum class LoudnessStandard(
    val displayName: String,
    val targetLufs: Float?,
    val description: String
) {
    OFF("Desativado", null, "Reproduz o áudio com a dinâmica original do arquivo sem processamento"),
    SPOTIFY("Spotify / Streaming (-14 LUFS)", -14f, "Padrão de streaming recomendado para equilíbrio e clareza"),
    APPLE_MUSIC("Apple Music / Podcasts (-16 LUFS)", -16f, "Maior alcance dinâmico com graves preservados"),
    CD_STANDARD("Padrão CD / Master Dinâmico (-11 LUFS)", -11f, "Equilíbrio entre fidelidade e intensidade sonora clássica"),
    CD_LOUD("CD Moderno / Alta Potência (-9 LUFS)", -9f, "Máxima pressão sonora para fones e ambientes ruidosos"),
    BROADCAST_EBU("EBU R128 / Broadcast (-23 LUFS)", -23f, "Padrão europeu de transmissão com alta faixa dinâmica")
}

/**
 * Modos de economia de dados móveis (idêntico ao Spotify 9.1.94)
 */
enum class DataSaverMode(val displayName: String, val description: String) {
    ALWAYS_OFF("Desativado", "Qualidade padrão selecionada"),
    AUTOMATIC("Automático", "Ajusta conforme a rede (reduz consumo em dados móveis)"),
    ALWAYS_ON("Sempre ativado", "Qualidade baixa permanente e economia de dados")
}

/**
 * Níveis de qualidade de streaming de áudio (idêntico ao Spotify 9.1.94)
 */
enum class AudioQuality(val displayName: String, val bitrateLabel: String) {
    AUTOMATIC("Automática", "Recomendado (ajusta conforme a rede)"),
    LOW("Baixa", "Economia máxima (~48-64 kbps)"),
    NORMAL("Normal", "Qualidade padrão (~128 kbps)"),
    HIGH("Alta", "Alta fidelidade (~160 kbps)"),
    VERY_HIGH("Muito alta", "Máxima fidelidade disponível")
}

/**
 * Níveis de qualidade para downloads offline.
 * Por padrão: LOSSLESS (áudio nativo de máxima fidelidade sem compressão adicional).
 */
enum class DownloadAudioQuality(val displayName: String, val description: String) {
    LOSSLESS("Lossless (Máxima Fidelidade)", "Áudio original sem perda perceptível de estúdio (Padrão)"),
    VERY_HIGH("Muito Alta (~320 kbps eq.)", "Altíssima fidelidade para fones avançados"),
    HIGH("Alta (256 kbps)", "Excelente qualidade e bom equilíbrio de espaço"),
    NORMAL("Normal (128 kbps)", "Economia máxima de armazenamento")
}

interface UserSettingsRepository {
    val isCanvasEnabled: StateFlow<Boolean>
    val isCanvasWifiOnly: StateFlow<Boolean>
    val isLyricsAboveTitleEnabled: StateFlow<Boolean>
    val isLibraryGridView: StateFlow<Boolean>

    // Transições de Faixa (Track Transitions) e DSP
    val isGaplessEnabled: StateFlow<Boolean>
    val isAutomixEnabled: StateFlow<Boolean>
    val crossfadeSeconds: StateFlow<Int>
    val loudnessStandard: StateFlow<LoudnessStandard>

    // Economia de Dados (Data-saving and offline)
    val dataSaverMode: StateFlow<DataSaverMode>
    val isDownloadOverCellular: StateFlow<Boolean>

    // Qualidade de Mídia (Media Quality)
    val wifiAudioQuality: StateFlow<AudioQuality>
    val cellularAudioQuality: StateFlow<AudioQuality>
    val downloadAudioQuality: StateFlow<DownloadAudioQuality>

    // Opções Granulares de Download (Canvas & Lyrics)
    val isDownloadCanvasEnabled: StateFlow<Boolean>
    val isDownloadLyricsEnabled: StateFlow<Boolean>

    // Smart Downloads (Downloads Inteligentes - YouTube Music Style)
    val isSmartDownloadsEnabled: StateFlow<Boolean>
    val smartDownloadsLimit: StateFlow<Int>
    val isSmartDownloadsWifiOnly: StateFlow<Boolean>

    // Primeiro Acesso e Onboarding do Usuário
    val isOnboardingCompleted: StateFlow<Boolean>

    fun setCanvasEnabled(enabled: Boolean)
    fun setCanvasWifiOnly(wifiOnly: Boolean)
    fun setLyricsAboveTitleEnabled(enabled: Boolean)
    fun setLibraryGridView(isGrid: Boolean)

    fun setGaplessEnabled(enabled: Boolean)
    fun setAutomixEnabled(enabled: Boolean)
    fun setCrossfadeSeconds(seconds: Int)
    fun setLoudnessStandard(standard: LoudnessStandard)

    fun setDataSaverMode(mode: DataSaverMode)
    fun setDownloadOverCellular(enabled: Boolean)
    fun setWifiAudioQuality(quality: AudioQuality)
    fun setCellularAudioQuality(quality: AudioQuality)
    fun setDownloadAudioQuality(quality: DownloadAudioQuality)

    fun setDownloadCanvasEnabled(enabled: Boolean)
    fun setDownloadLyricsEnabled(enabled: Boolean)

    fun setSmartDownloadsEnabled(enabled: Boolean)
    fun setSmartDownloadsLimit(limit: Int)
    fun setSmartDownloadsWifiOnly(wifiOnly: Boolean)

    fun setOnboardingCompleted(completed: Boolean)
}

class UserSettingsRepositoryImpl(context: Context) : UserSettingsRepository {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isCanvasEnabled = MutableStateFlow(prefs.getBoolean(KEY_CANVAS_ENABLED, true))
    override val isCanvasEnabled: StateFlow<Boolean> = _isCanvasEnabled.asStateFlow()

    private val _isCanvasWifiOnly = MutableStateFlow(prefs.getBoolean(KEY_CANVAS_WIFI_ONLY, true))
    override val isCanvasWifiOnly: StateFlow<Boolean> = _isCanvasWifiOnly.asStateFlow()

    private val _isLyricsAboveTitleEnabled = MutableStateFlow(prefs.getBoolean(KEY_LYRICS_ABOVE_TITLE, true))
    override val isLyricsAboveTitleEnabled: StateFlow<Boolean> = _isLyricsAboveTitleEnabled.asStateFlow()

    private val _isLibraryGridView = MutableStateFlow(prefs.getBoolean(KEY_LIBRARY_GRID_VIEW, true))
    override val isLibraryGridView: StateFlow<Boolean> = _isLibraryGridView.asStateFlow()

    private val _isGaplessEnabled = MutableStateFlow(prefs.getBoolean(KEY_GAPLESS_ENABLED, true))
    override val isGaplessEnabled: StateFlow<Boolean> = _isGaplessEnabled.asStateFlow()

    private val _isAutomixEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTOMIX_ENABLED, true))
    override val isAutomixEnabled: StateFlow<Boolean> = _isAutomixEnabled.asStateFlow()

    private val _crossfadeSeconds = MutableStateFlow(prefs.getInt(KEY_CROSSFADE_SECONDS, 0))
    override val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    private val _loudnessStandard = MutableStateFlow(loadLoudnessStandard())
    override val loudnessStandard: StateFlow<LoudnessStandard> = _loudnessStandard.asStateFlow()

    private val _dataSaverMode = MutableStateFlow(loadDataSaverMode())
    override val dataSaverMode: StateFlow<DataSaverMode> = _dataSaverMode.asStateFlow()

    private val _isDownloadOverCellular = MutableStateFlow(prefs.getBoolean(KEY_DOWNLOAD_OVER_CELLULAR, false))
    override val isDownloadOverCellular: StateFlow<Boolean> = _isDownloadOverCellular.asStateFlow()

    private val _wifiAudioQuality = MutableStateFlow(loadWifiAudioQuality())
    override val wifiAudioQuality: StateFlow<AudioQuality> = _wifiAudioQuality.asStateFlow()

    private val _cellularAudioQuality = MutableStateFlow(loadCellularAudioQuality())
    override val cellularAudioQuality: StateFlow<AudioQuality> = _cellularAudioQuality.asStateFlow()

    private val _downloadAudioQuality = MutableStateFlow(loadDownloadAudioQuality())
    override val downloadAudioQuality: StateFlow<DownloadAudioQuality> = _downloadAudioQuality.asStateFlow()

    private val _isDownloadCanvasEnabled = MutableStateFlow(prefs.getBoolean(KEY_DOWNLOAD_CANVAS_ENABLED, false))
    override val isDownloadCanvasEnabled: StateFlow<Boolean> = _isDownloadCanvasEnabled.asStateFlow()

    private val _isDownloadLyricsEnabled = MutableStateFlow(prefs.getBoolean(KEY_DOWNLOAD_LYRICS_ENABLED, true))
    override val isDownloadLyricsEnabled: StateFlow<Boolean> = _isDownloadLyricsEnabled.asStateFlow()

    private val _isSmartDownloadsEnabled = MutableStateFlow(prefs.getBoolean(KEY_SMART_DOWNLOADS_ENABLED, false))
    override val isSmartDownloadsEnabled: StateFlow<Boolean> = _isSmartDownloadsEnabled.asStateFlow()

    private val _smartDownloadsLimit = MutableStateFlow(prefs.getInt(KEY_SMART_DOWNLOADS_LIMIT, 100))
    override val smartDownloadsLimit: StateFlow<Int> = _smartDownloadsLimit.asStateFlow()

    private val _isSmartDownloadsWifiOnly = MutableStateFlow(prefs.getBoolean(KEY_SMART_DOWNLOADS_WIFI_ONLY, true))
    override val isSmartDownloadsWifiOnly: StateFlow<Boolean> = _isSmartDownloadsWifiOnly.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false))
    override val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_CANVAS_ENABLED -> _isCanvasEnabled.value = prefs.getBoolean(KEY_CANVAS_ENABLED, true)
            KEY_CANVAS_WIFI_ONLY -> _isCanvasWifiOnly.value = prefs.getBoolean(KEY_CANVAS_WIFI_ONLY, true)
            KEY_LYRICS_ABOVE_TITLE -> _isLyricsAboveTitleEnabled.value = prefs.getBoolean(KEY_LYRICS_ABOVE_TITLE, true)
            KEY_LIBRARY_GRID_VIEW -> _isLibraryGridView.value = prefs.getBoolean(KEY_LIBRARY_GRID_VIEW, true)
            KEY_GAPLESS_ENABLED -> _isGaplessEnabled.value = prefs.getBoolean(KEY_GAPLESS_ENABLED, true)
            KEY_AUTOMIX_ENABLED -> _isAutomixEnabled.value = prefs.getBoolean(KEY_AUTOMIX_ENABLED, true)
            KEY_CROSSFADE_SECONDS -> _crossfadeSeconds.value = prefs.getInt(KEY_CROSSFADE_SECONDS, 0)
            KEY_LOUDNESS_STANDARD -> _loudnessStandard.value = loadLoudnessStandard()
            KEY_DATA_SAVER_MODE -> _dataSaverMode.value = loadDataSaverMode()
            KEY_DOWNLOAD_OVER_CELLULAR -> _isDownloadOverCellular.value = prefs.getBoolean(KEY_DOWNLOAD_OVER_CELLULAR, false)
            KEY_WIFI_AUDIO_QUALITY -> _wifiAudioQuality.value = loadWifiAudioQuality()
            KEY_CELLULAR_AUDIO_QUALITY -> _cellularAudioQuality.value = loadCellularAudioQuality()
            KEY_DOWNLOAD_AUDIO_QUALITY -> _downloadAudioQuality.value = loadDownloadAudioQuality()
            KEY_DOWNLOAD_CANVAS_ENABLED -> _isDownloadCanvasEnabled.value = prefs.getBoolean(KEY_DOWNLOAD_CANVAS_ENABLED, false)
            KEY_DOWNLOAD_LYRICS_ENABLED -> _isDownloadLyricsEnabled.value = prefs.getBoolean(KEY_DOWNLOAD_LYRICS_ENABLED, true)
            KEY_SMART_DOWNLOADS_ENABLED -> _isSmartDownloadsEnabled.value = prefs.getBoolean(KEY_SMART_DOWNLOADS_ENABLED, false)
            KEY_SMART_DOWNLOADS_LIMIT -> _smartDownloadsLimit.value = prefs.getInt(KEY_SMART_DOWNLOADS_LIMIT, 100)
            KEY_SMART_DOWNLOADS_WIFI_ONLY -> _isSmartDownloadsWifiOnly.value = prefs.getBoolean(KEY_SMART_DOWNLOADS_WIFI_ONLY, true)
            KEY_ONBOARDING_COMPLETED -> _isOnboardingCompleted.value = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private fun loadLoudnessStandard(): LoudnessStandard {
        val raw = prefs.getString(KEY_LOUDNESS_STANDARD, LoudnessStandard.SPOTIFY.name)
        return try {
            LoudnessStandard.valueOf(raw ?: LoudnessStandard.SPOTIFY.name)
        } catch (_: Exception) {
            LoudnessStandard.SPOTIFY
        }
    }

    private fun loadDataSaverMode(): DataSaverMode {
        val raw = prefs.getString(KEY_DATA_SAVER_MODE, DataSaverMode.ALWAYS_OFF.name)
        return try {
            DataSaverMode.valueOf(raw ?: DataSaverMode.ALWAYS_OFF.name)
        } catch (_: Exception) {
            DataSaverMode.ALWAYS_OFF
        }
    }

    private fun loadWifiAudioQuality(): AudioQuality {
        val raw = prefs.getString(KEY_WIFI_AUDIO_QUALITY, AudioQuality.HIGH.name)
        return try {
            AudioQuality.valueOf(raw ?: AudioQuality.HIGH.name)
        } catch (_: Exception) {
            AudioQuality.HIGH
        }
    }

    private fun loadCellularAudioQuality(): AudioQuality {
        val raw = prefs.getString(KEY_CELLULAR_AUDIO_QUALITY, AudioQuality.AUTOMATIC.name)
        return try {
            AudioQuality.valueOf(raw ?: AudioQuality.AUTOMATIC.name)
        } catch (_: Exception) {
            AudioQuality.AUTOMATIC
        }
    }

    private fun loadDownloadAudioQuality(): DownloadAudioQuality {
        val raw = prefs.getString(KEY_DOWNLOAD_AUDIO_QUALITY, DownloadAudioQuality.LOSSLESS.name)
        return try {
            DownloadAudioQuality.valueOf(raw ?: DownloadAudioQuality.LOSSLESS.name)
        } catch (_: Exception) {
            DownloadAudioQuality.LOSSLESS
        }
    }

    override fun setCanvasEnabled(enabled: Boolean) {
        _isCanvasEnabled.value = enabled
        prefs.edit().putBoolean(KEY_CANVAS_ENABLED, enabled).apply()
    }

    override fun setCanvasWifiOnly(wifiOnly: Boolean) {
        _isCanvasWifiOnly.value = wifiOnly
        prefs.edit().putBoolean(KEY_CANVAS_WIFI_ONLY, wifiOnly).apply()
    }

    override fun setLyricsAboveTitleEnabled(enabled: Boolean) {
        _isLyricsAboveTitleEnabled.value = enabled
        prefs.edit().putBoolean(KEY_LYRICS_ABOVE_TITLE, enabled).apply()
    }

    override fun setLibraryGridView(isGrid: Boolean) {
        _isLibraryGridView.value = isGrid
        prefs.edit().putBoolean(KEY_LIBRARY_GRID_VIEW, isGrid).apply()
    }

    override fun setGaplessEnabled(enabled: Boolean) {
        _isGaplessEnabled.value = enabled
        prefs.edit().putBoolean(KEY_GAPLESS_ENABLED, enabled).apply()
    }

    override fun setAutomixEnabled(enabled: Boolean) {
        _isAutomixEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUTOMIX_ENABLED, enabled).apply()
    }

    override fun setCrossfadeSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(0, 12)
        _crossfadeSeconds.value = clamped
        prefs.edit().putInt(KEY_CROSSFADE_SECONDS, clamped).apply()
    }

    override fun setLoudnessStandard(standard: LoudnessStandard) {
        _loudnessStandard.value = standard
        prefs.edit().putString(KEY_LOUDNESS_STANDARD, standard.name).apply()
    }

    override fun setDataSaverMode(mode: DataSaverMode) {
        _dataSaverMode.value = mode
        prefs.edit().putString(KEY_DATA_SAVER_MODE, mode.name).apply()
    }

    override fun setDownloadOverCellular(enabled: Boolean) {
        _isDownloadOverCellular.value = enabled
        prefs.edit().putBoolean(KEY_DOWNLOAD_OVER_CELLULAR, enabled).apply()
    }

    override fun setWifiAudioQuality(quality: AudioQuality) {
        _wifiAudioQuality.value = quality
        prefs.edit().putString(KEY_WIFI_AUDIO_QUALITY, quality.name).apply()
    }

    override fun setCellularAudioQuality(quality: AudioQuality) {
        _cellularAudioQuality.value = quality
        prefs.edit().putString(KEY_CELLULAR_AUDIO_QUALITY, quality.name).apply()
    }

    override fun setDownloadAudioQuality(quality: DownloadAudioQuality) {
        _downloadAudioQuality.value = quality
        prefs.edit().putString(KEY_DOWNLOAD_AUDIO_QUALITY, quality.name).apply()
    }

    override fun setDownloadCanvasEnabled(enabled: Boolean) {
        _isDownloadCanvasEnabled.value = enabled
        prefs.edit().putBoolean(KEY_DOWNLOAD_CANVAS_ENABLED, enabled).apply()
    }

    override fun setDownloadLyricsEnabled(enabled: Boolean) {
        _isDownloadLyricsEnabled.value = enabled
        prefs.edit().putBoolean(KEY_DOWNLOAD_LYRICS_ENABLED, enabled).apply()
    }

    override fun setSmartDownloadsEnabled(enabled: Boolean) {
        _isSmartDownloadsEnabled.value = enabled
        prefs.edit().putBoolean(KEY_SMART_DOWNLOADS_ENABLED, enabled).apply()
    }

    override fun setSmartDownloadsLimit(limit: Int) {
        val valid = limit.coerceIn(10, 1000)
        _smartDownloadsLimit.value = valid
        prefs.edit().putInt(KEY_SMART_DOWNLOADS_LIMIT, valid).apply()
    }

    override fun setSmartDownloadsWifiOnly(wifiOnly: Boolean) {
        _isSmartDownloadsWifiOnly.value = wifiOnly
        prefs.edit().putBoolean(KEY_SMART_DOWNLOADS_WIFI_ONLY, wifiOnly).apply()
    }

    override fun setOnboardingCompleted(completed: Boolean) {
        _isOnboardingCompleted.value = completed
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    companion object {
        private const val PREFS_NAME = "nerf_user_settings_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_CANVAS_ENABLED = "key_canvas_video_enabled"
        private const val KEY_CANVAS_WIFI_ONLY = "key_canvas_wifi_only"
        private const val KEY_LYRICS_ABOVE_TITLE = "key_lyrics_above_title"
        private const val KEY_LIBRARY_GRID_VIEW = "key_library_grid_view"
        private const val KEY_GAPLESS_ENABLED = "key_gapless_enabled"
        private const val KEY_AUTOMIX_ENABLED = "key_automix_enabled"
        private const val KEY_CROSSFADE_SECONDS = "key_crossfade_seconds"
        private const val KEY_LOUDNESS_STANDARD = "key_loudness_standard"
        private const val KEY_DATA_SAVER_MODE = "key_data_saver_mode"
        private const val KEY_DOWNLOAD_OVER_CELLULAR = "key_download_over_cellular"
        private const val KEY_WIFI_AUDIO_QUALITY = "key_wifi_audio_quality"
        private const val KEY_CELLULAR_AUDIO_QUALITY = "key_cellular_audio_quality"
        private const val KEY_DOWNLOAD_AUDIO_QUALITY = "key_download_audio_quality"
        private const val KEY_DOWNLOAD_CANVAS_ENABLED = "key_download_canvas_enabled"
        private const val KEY_DOWNLOAD_LYRICS_ENABLED = "key_download_lyrics_enabled"
        private const val KEY_SMART_DOWNLOADS_ENABLED = "key_smart_downloads_enabled"
        private const val KEY_SMART_DOWNLOADS_LIMIT = "key_smart_downloads_limit"
        private const val KEY_SMART_DOWNLOADS_WIFI_ONLY = "key_smart_downloads_wifi_only"
    }
}
