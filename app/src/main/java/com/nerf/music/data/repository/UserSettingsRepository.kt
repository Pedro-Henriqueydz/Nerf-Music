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

    fun setCanvasEnabled(enabled: Boolean)
    fun setCanvasWifiOnly(wifiOnly: Boolean)
    fun setLyricsAboveTitleEnabled(enabled: Boolean)
    fun setLibraryGridView(isGrid: Boolean)
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

    companion object {
        private const val PREFS_NAME = "nerf_user_settings_prefs"
        private const val KEY_CANVAS_ENABLED = "key_canvas_video_enabled"
        private const val KEY_CANVAS_WIFI_ONLY = "key_canvas_wifi_only"
        private const val KEY_LYRICS_ABOVE_TITLE = "key_lyrics_above_title"
        private const val KEY_LIBRARY_GRID_VIEW = "key_library_grid_view"
    }
}
