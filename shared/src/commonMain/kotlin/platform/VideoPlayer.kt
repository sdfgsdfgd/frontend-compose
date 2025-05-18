// shared/src/commonMain/kotlin/net/sdfgsdfg/VideoPlayer.kt
package net.sdfgsdfg.platform

import androidx.compose.runtime.Composable

interface VideoPlayer {
    fun play(source: String)
    fun pause()
    fun stop()
}

@Composable
expect fun videoPlayer(): VideoPlayer