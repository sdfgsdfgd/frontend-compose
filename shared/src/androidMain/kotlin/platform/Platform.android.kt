package net.sdfgsdfg.platform

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.jetbrains.compose.resources.DrawableResource
import androidx.core.net.toUri

@Composable
actual fun Video(
    source   : DrawableResource,
    modifier : Modifier,
    autoPlay : Boolean
) {
    val ctx  = LocalContext.current
    val uri  = remember(ctx, source) { source.playablePath(ctx).toUri() }
    val exo  = remember { ExoPlayer.Builder(ctx).build() }

    DisposableEffect(exo) { onDispose { exo.release() } }

    LaunchedEffect(uri, autoPlay) {
        exo.setMediaItem(MediaItem.fromUri(uri))
        exo.prepare()
        if (autoPlay) exo.play()
    }

    AndroidView(
        factory  = { PlayerView(it).apply { player = exo } },
        modifier = modifier
    )
}
