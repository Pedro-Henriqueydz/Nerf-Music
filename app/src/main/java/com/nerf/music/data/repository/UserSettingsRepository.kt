package com.nerf.music.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface UserSettingsRepository {
    val isCanvasEnabled: StateFlow<Boolean>
    val isCanvasWifiOnly: StateFlow<Boolean>
    val isLyricsAboveTitleEnabled: StateFlow<Boolean>
    val isLibraryGridView: StateFlow<Boolean>

    // Transições de Faixa (Track Transitions)
    val isGaplessEnabled: StateFlow<Boolean>
    val isAutomixEnabled: StateFlow<Boolean>
    val crossfadeSeconds: StateFlow<Int>

    fun setCanvasEnabled(enabled: Boolean)
    fun setCanvasWifiOnly(wifiOnly: Boolean)
    fun setLyricsAboveTitleEnabled(enabled: Boolean)
    fun setLibraryGridView(isGrid: Boolean)

    fun setGaplessEnabled(enabled: Boolean)
    fun setAutomixEnabled(enabled: Boolean)
    fun setCrossfadeSeconds(seconds: Int)
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

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_CANVAS_ENABLED -> _isCanvasEnabled.value = prefs.getBoolean(KEY_CANVAS_ENABLED, true)
            KEY_CANVAS_WIFI_ONLY -> _isCanvasWifiOnly.value = prefs.getBoolean(KEY_CANVAS_WIFI_ONLY, true)
            KEY_LYRICS_ABOVE_TITLE -> _isLyricsAboveTitleEnabled.value = prefs.getBoolean(KEY_LYRICS_ABOVE_TITLE, true)
            KEY_LIBRARY_GRID_VIEW -> _isLibraryGridView.value = prefs.getBoolean(KEY_LIBRARY_GRID_VIEW, true)
            KEY_GAPLESS_ENABLED -> _isGaplessEnabled.value = prefs.getBoolean(KEY_GAPLESS_ENABLED, true)
            KEY_AUTOMIX_ENABLED -> _isAutomixEnabled.value = prefs.getBoolean(KEY_AUTOMIX_ENABLED, true)
            KEY_CROSSFADE_SECONDS -> _crossfadeSeconds.value = prefs.getInt(KEY_CROSSFADE_SECONDS, 0)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
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

    companion object {
        private const val PREFS_NAME = "nerf_user_settings_prefs"
        private const val KEY_CANVAS_ENABLED = "key_canvas_video_enabled"
        private const val KEY_CANVAS_WIFI_ONLY = "key_canvas_wifi_only"
        private const val KEY_LYRICS_ABOVE_TITLE = "key_lyrics_above_title"
        private const val KEY_LIBRARY_GRID_VIEW = "key_library_grid_view"
        private const val KEY_GAPLESS_ENABLED = "key_gapless_enabled"
        private const val KEY_AUTOMIX_ENABLED = "key_automix_enabled"
        private const val KEY_CROSSFADE_SECONDS = "key_crossfade_seconds"
    }
}
