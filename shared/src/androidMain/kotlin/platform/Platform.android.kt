package platform


import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import okio.Path
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.DrawableResource
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

// region BrowserLauncher
actual val REDIRECT      = "https://sdfgsdfg.net/api/auth/callback/github"
actual val STATE_PREFIX  = "mob-"

actual object BrowserLauncher {
    actual fun open(url: String, platformCtx: Any) {
        val ctx: Context = when (platformCtx) {
            is Context -> platformCtx
            is PlatformContext -> platformCtx.ctx
            else -> error("Unsupported platform context: $platformCtx")
        }

        CustomTabsIntent.Builder()
            .build()
            .launchUrl(ctx, url.toUri())
    }
}

actual fun sha256(bytes: ByteArray): ByteArray = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)

actual object DeepLinkHandler {
    private val ch = Channel<String>(capacity = Channel.CONFLATED)
    @kotlin.OptIn(DelicateCoroutinesApi::class)
    actual val uriFlow: SharedFlow<String> = ch.receiveAsFlow().shareIn(
        scope = kotlinx.coroutines.GlobalScope,
        started = kotlinx.coroutines.flow.SharingStarted.Lazily,
        replay = 1
    )
    fun onNewUri(uri: String) {
        ch.trySend(uri)
    }
}

actual object AppDirs {
    private lateinit var internal: Path

    /** MUST be called from Application.onCreate() */
    actual fun init(platformCtx: Any?) {
        val ctx = platformCtx as Context
        val dir = ctx.getDir("arcana", Context.MODE_PRIVATE)
        internal = dir.absolutePath.toPath()
    }

    actual val path: Path
        get() = if (::internal.isInitialized) internal
        else error("AppDirs.init(ctx) not called yet")
}
