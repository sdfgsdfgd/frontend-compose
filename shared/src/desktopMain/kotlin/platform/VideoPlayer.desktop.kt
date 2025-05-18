// shared/src/desktopMain/kotlin/net/sdfgsdfg/VideoPlayer.desktop.kt
package net.sdfgsdfg.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import utils.initializeMediaPlayerComponent
import java.awt.Component

private class VlcjVideoPlayer(private val mp: MediaPlayer) : VideoPlayer {
    override fun play(source: String) {
        /* off-load I/O to VLCJ thread-pool to avoid jank */
        mp.submit { mp.media().play(source) }
    }
    override fun pause() = mp.submit { mp.controls().pause() }
    override fun stop()  = mp.submit { mp.controls().stop() }
}

@Composable
actual fun videoPlayer(): VideoPlayer {
    /* single MediaPlayer per remember() scope */
    val component = remember { initializeMediaPlayerComponent() }
    val media     = remember { component.mediaPlayer() }
    return remember { VlcjVideoPlayer(media) }
}

/* helper so both components share the same call‐site */
private fun Component.mediaPlayer(): MediaPlayer = when (this) {
    is CallbackMediaPlayerComponent -> mediaPlayer()
    is EmbeddedMediaPlayerComponent -> mediaPlayer()
    else -> error("Unexpected VLCJ component")
}