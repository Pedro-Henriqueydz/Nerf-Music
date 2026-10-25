package com.nerf.music

import com.nerf.music.player.CompositeStreamResolver
import com.nerf.music.player.InnerTubeStreamResolver
import com.nerf.music.player.NewPipeStreamResolver
import com.nerf.music.player.createDefaultStreamResolver
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultStreamResolverTest {

    @Test
    fun defaultResolverPrefersQualityAwareNewPipeAndKeepsInnerTubeFallback() {
        val resolver = createDefaultStreamResolver(context = null) as CompositeStreamResolver

        assertTrue(resolver.primary is NewPipeStreamResolver)
        assertTrue(resolver.secondary is InnerTubeStreamResolver)
    }
}
