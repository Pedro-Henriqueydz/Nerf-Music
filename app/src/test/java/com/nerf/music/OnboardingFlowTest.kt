package com.nerf.music

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.nerf.music.data.repository.UserAffinityRepositoryImpl
import com.nerf.music.data.repository.UserHistoryRepositoryImpl
import com.nerf.music.data.repository.UserSettingsRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap

/**
 * Testes unitários para o fluxo de Onboarding e inicialização de preferências de gosto musical.
 */
class OnboardingFlowTest {

    private val prefsStore = ConcurrentHashMap<String, Any>()

    @Before
    fun setUp() {
        prefsStore.clear()
    }

    private fun createMockContext(): Context {
        val dummyPrefs = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java, SharedPreferences.Editor::class.java),
            object : InvocationHandler {
                override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
                    val key = args?.getOrNull(0) as? String
                    return when (method.name) {
                        "getString" -> (prefsStore[key] as? String) ?: args?.getOrNull(1)
                        "getBoolean" -> (prefsStore[key] as? Boolean) ?: args?.getOrNull(1) ?: false
                        "getInt" -> (prefsStore[key] as? Int) ?: args?.getOrNull(1) ?: 0
                        "getLong" -> (prefsStore[key] as? Long) ?: args?.getOrNull(1) ?: 0L
                        "contains" -> prefsStore.containsKey(key)
                        "registerOnSharedPreferenceChangeListener",
                        "unregisterOnSharedPreferenceChangeListener" -> null
                        "edit" -> proxy
                        "putString" -> {
                            val v = args?.getOrNull(1) as? String
                            if (key != null && v != null) prefsStore[key] = v
                            proxy
                        }
                        "putBoolean" -> {
                            val v = args?.getOrNull(1) as? Boolean
                            if (key != null && v != null) prefsStore[key] = v
                            proxy
                        }
                        "putInt" -> {
                            val v = args?.getOrNull(1) as? Int
                            if (key != null && v != null) prefsStore[key] = v
                            proxy
                        }
                        "apply", "commit" -> null
                        else -> null
                    }
                }
            }
        ) as SharedPreferences

        return object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = dummyPrefs
            override fun getPackageName(): String = "com.nerf.music"
        }
    }

    @Test
    fun testOnboardingCompletedFlagPersistence() {
        val context = createMockContext()
        val settingsRepo = UserSettingsRepositoryImpl(context)

        // Por padrão no primeiro acesso, deve ser falso
        assertFalse("Onboarding deve iniciar como não completado", settingsRepo.isOnboardingCompleted.value)

        // Usuário completa onboarding
        settingsRepo.setOnboardingCompleted(true)
        assertTrue("Onboarding deve ser marcado como completado", settingsRepo.isOnboardingCompleted.value)

        // Recriação do repositório deve preservar o estado salvo
        val reloadedSettings = UserSettingsRepositoryImpl(context)
        assertTrue("Estado do onboarding deve persistir", reloadedSettings.isOnboardingCompleted.value)
    }

    @Test
    fun testInitialArtistPreferencesInUserAffinity() {
        val context = createMockContext()
        val affinityRepo = UserAffinityRepositoryImpl(context)

        val selectedArtists = listOf("Taylor Swift", "The Weeknd", "Coldplay")
        affinityRepo.recordInitialArtistPreferences(selectedArtists)

        // Verifica que todos os artistas selecionados ganharam pontuação positiva de afinidade
        assertTrue(affinityRepo.getArtistAffinity("Taylor Swift") > 10)
        assertTrue(affinityRepo.getArtistAffinity("The Weeknd") > 10)
        assertTrue(affinityRepo.getArtistAffinity("Coldplay") > 10)

        val topArtists = affinityRepo.getTopAffinitiveArtists(5)
        assertTrue(topArtists.contains("Taylor Swift"))
        assertTrue(topArtists.contains("The Weeknd"))
        assertTrue(topArtists.contains("Coldplay"))
    }

    @Test
    fun initialAffinityIsEqualAndDoesNotResetAfterListeningOrReload() {
        val context = createMockContext()
        val repo = UserAffinityRepositoryImpl(context)
        repo.recordInitialArtistPreferences(listOf("A", "B", "C"))
        assertEquals(listOf(100, 100, 100), listOf("A", "B", "C").map { repo.getArtistAffinity(it) })
        val track = com.nerf.music.data.model.Track(id = "song", title = "Song", artist = "A")
        repo.recordTrackEnded(track, 1000, 180000, true)
        repo.recordInitialArtistPreferences(listOf("A", "B", "C"))
        assertEquals(99, repo.getArtistAffinity("A"))
        assertEquals(99, UserAffinityRepositoryImpl(context).getArtistAffinity("A"))
    }

    @Test
    fun singleSkipDoesNotBlockArtistButRepeatedSkipsDo() {
        val repo = UserAffinityRepositoryImpl(createMockContext())
        val track = com.nerf.music.data.model.Track(id = "song", title = "Song", artist = "A")
        repo.recordTrackEnded(track, 1000, 180000, true)
        assertFalse(repo.getPenalizedArtists().contains("A"))
        repeat(2) { repo.recordTrackEnded(track, 1000, 180000, true) }
        assertTrue(repo.getPenalizedArtists().contains("A"))
    }

    @Test
    fun testInitialArtistsSeedingInUserHistory() {
        val context = createMockContext()
        val historyRepo = UserHistoryRepositoryImpl(context)

        val selectedArtists = listOf("Dua Lipa", "Imagine Dragons", "Bruno Mars")
        historyRepo.recordInitialArtists(selectedArtists)

        val topArtists = historyRepo.getTopArtists(5)
        assertEquals(3, topArtists.size)
        assertEquals("Dua Lipa", topArtists[0])
        assertEquals("Imagine Dragons", topArtists[1])
        assertEquals("Bruno Mars", topArtists[2])
    }
}
