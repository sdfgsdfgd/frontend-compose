package net.sdfgsdfg.platform

import androidx.compose.runtime.Composable
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.Foundation.NSURL

private class AvVideoPlayer : VideoPlayer {
    private val player = AVPlayer()

    override fun play(source: String) {
        val item = AVPlayerItem(uRL = NSURL(string = source))
        player.replaceCurrentItemWithPlayerItem(item)
        player.play()
    }

    override fun pause() = player.pause()
    override fun stop()  = player.pause()
}

@Composable
actual fun videoPlayer(): VideoPlayer = AvVideoPlayer()