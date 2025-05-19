package net.sdfgsdfg.platform

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import net.sdfgsdfg.R
import net.sdfgsdfg.resources.Res
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import java.io.File

@OptIn(UnstableApi::class)
@Composable
actual fun Video(
    source: DrawableResource,
    modifier: Modifier,
    autoPlay: Boolean
) {
    val ctx = LocalContext.current
    val path = remember { source.toPlayablePath() }        // ← temp file
    Log.d("XXX", "temp file : $path")                    // sanity check

    val uri = remember { Uri.fromFile(File(path)) }    // file://…
    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(uri, autoPlay) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = autoPlay
    }

    AndroidView(
        factory = {
            PlayerView(ctx).also {
                it.player = player
                it.useController = false
                it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                it.setBackgroundColor(Color.Transparent.toArgb())
            }
        },
        modifier = modifier
    )
}
