package platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.uri
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.DrawableResource
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.InputEvents
import utils.isMacOS
import utils.mediaPlayer
import utils.toPlayablePath
import java.awt.Component
import java.awt.Desktop.getDesktop
import java.net.URI

/**
 * Desktop doesn’t need anything – an empty object is enough.
 */
actual class PlatformContext

actual val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext> =
    staticCompositionLocalOf { PlatformContext() }

actual val LocalWindowMetrics = compositionLocalOf {
    WindowMetrics(DpSize.Zero, IntSize.Zero, 1f)
}

@Composable
actual fun rememberWindowMetrics(): WindowMetrics {
    val info = LocalWindowInfo.current
    val density = LocalDensity.current
    val sizeDp = with(density) {
        DpSize(info.containerSize.width.toDp(), info.containerSize.height.toDp())
    }
    return remember(info, density) {
        WindowMetrics(sizeDp, info.containerSize, density.density)
    }
}

@Composable
actual fun Video(
    source: DrawableResource,
    modifier: Modifier,
    autoPlay: Boolean
) {
    val url = remember { source.toPlayablePath() }
    val comp = remember { initializeMediaPlayerComponent() }
    val mp = remember { comp.mediaPlayer() }

    LaunchedEffect(url, autoPlay) {
        println("Playing video: $url")

        mp.controls().repeat = true
        mp.media().play(url, ":input-repeat=65535")   // loop forever

        if (!autoPlay) mp.controls().pause()
    }

    SwingPanel(factory = { comp }, modifier = modifier)
}

/**
 * See https://github.com/caprica/vlcj/issues/887#issuecomment-503288294
 * for why we're using CallbackMediaPlayerComponent for macOS.
 */
fun initializeMediaPlayerComponent(): Component {
    NativeDiscovery().discover()
    return if (isMacOS()) {
        CallbackMediaPlayerComponent(
            MediaPlayerFactory("--no-play-and-pause"), null,
            InputEvents.NONE, true, null, null, null, null,
        )
    } else {
        EmbeddedMediaPlayerComponent("--no-play-and-pause")
    }
}

actual val REDIRECT = "http://localhost:1410/callback"
actual val STATE_PREFIX = ""          // no prefix needed

actual object BrowserLauncher {
    actual fun open(url: String, platformCtx: Any) {
        // 1) start one-shot server (port 1410)
        embeddedServer(CIO, host = "127.0.0.1", port = 1410) {
            routing {
                get("/callback") {
                    call.respondText("✔ Login complete — you may close this tab.")
                    DeepLinkHandler.emit(call.request.uri)
                    launch { delay(300); this@embeddedServer.dispose() }
                }
            }
        }.start(false)

        runCatching { getDesktop().browse(URI(url)) }
            .onFailure {
                Runtime.getRuntime().exec(arrayOf("xdg-open", url))
                println("Failed to open browser: ${it.message}")
            }.onSuccess {
                println("Opened browser to: $url")
            }
    }
}

actual fun sha256(bytes: ByteArray): ByteArray = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)

actual object DeepLinkHandler {
    private val _flow = MutableSharedFlow<String>(replay = 1)
    actual val uriFlow = _flow.asSharedFlow()
    internal fun emit(uri: String) {
        _flow.tryEmit(uri)
    }
}

actual object AppDirs {
    actual fun init(platformCtx: Any?) {}

    actual val path: Path by lazy {
        val p = (System.getProperty("user.home") + "/.arcana").toPath()
        FileSystem.SYSTEM.createDirectories(p)
        p
    }
}
