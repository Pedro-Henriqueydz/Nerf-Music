package com.nerf.music

import com.nerf.music.data.repository.AudioQuality
import com.nerf.music.player.CompositeStreamResolver
import com.nerf.music.player.InnerTubeStreamResolver
import com.nerf.music.player.NewPipeStreamResolver
import com.nerf.music.player.StreamResolver
import com.nerf.music.player.createDefaultStreamResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultStreamResolverTest {

    @Test
    fun defaultResolverPrefersQualityAwareNewPipeAndKeepsInnerTubeFallback() {
        val resolver = createDefaultStreamResolver(context = null) as CompositeStreamResolver

        assertTrue(resolver.primary is NewPipeStreamResolver)
        assertTrue(resolver.secondary is InnerTubeStreamResolver)
    }

    @Test
    fun compositeStreamResolverPassesTargetQualityToSecondaryOnPrimaryFailure() = runBlocking {
        var receivedPrimaryQuality: AudioQuality? = null
        var receivedSecondaryQuality: AudioQuality? = null

        val mockPrimary = object : StreamResolver {
            override suspend fun resolveStreamUrl(videoId: String): Result<String> = Result.failure(Exception("fail"))
            override suspend fun resolveStreamUrl(videoId: String, targetQuality: AudioQuality?): Result<String> {
                receivedPrimaryQuality = targetQuality
                return Result.failure(Exception("primary failed"))
            }
        }
        val mockSecondary = object : StreamResolver {
            override suspend fun resolveStreamUrl(videoId: String): Result<String> = Result.success("http://example.com/stream")
            override suspend fun resolveStreamUrl(videoId: String, targetQuality: AudioQuality?): Result<String> {
                receivedSecondaryQuality = targetQuality
                return Result.success("http://example.com/stream")
            }
        }

        val composite = CompositeStreamResolver(mockPrimary, mockSecondary)
        val result = composite.resolveStreamUrl("test_vid", AudioQuality.LOW)

        assertTrue(result.isSuccess)
        assertEquals(AudioQuality.LOW, receivedPrimaryQuality)
        assertEquals(AudioQuality.LOW, receivedSecondaryQuality)
    }
}
