package net.sdfgsdfg.platform

import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

//private class ExoVideoPlayer(val exo: ExoPlayer) : VideoPlayer {
//    override fun play(source: String) {
//        exo.setMediaItem(MediaItem.fromUri(source))
//        exo.prepare(); exo.play()
//    }
//    override fun pause() = exo.pause()
//    override fun stop()  = exo.stop()
//}

//@Composable
//actual fun videoPlayer(): VideoPlayer {
//    val ctx  = LocalContext.current
//    val exo  = remember { ExoPlayer.Builder(ctx).build() }
//    return remember { ExoVideoPlayer(exo) }
//}

//
//
//@Composable
//fun Video(
//    source : String,
//    modifier: Modifier = Modifier
//) {
//    val player = videoPlayer()
//
//    AndroidView(
//        factory = { ctx ->
//            PlayerView(ctx).apply {
//                layoutParams = FrameLayout.LayoutParams(
//                    FrameLayout.LayoutParams.MATCH_PARENT,
//                    FrameLayout.LayoutParams.MATCH_PARENT
//                )
//            }
//        },
//        modifier = modifier,
//        update  = { view ->
//            player.play(source)              // feed/update each recomposition // xx confirm
//            view.player = (player as ExoVideoPlayer).exo // xx ???? confirm w/ ai n remove
//        }
//    )
//}