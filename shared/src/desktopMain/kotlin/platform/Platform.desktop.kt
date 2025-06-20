package platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.uri
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.sdfgsdfg.zoomToFull
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
import java.awt.Frame
import java.awt.MouseInfo
import java.awt.Rectangle
import java.net.URI
import java.security.MessageDigest
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

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

actual fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

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

// TODO: Caching the TCC grant so you don’t try to register every launch—query GlobalScreen.isNativeHookRegistered
//    and skip the obnoxious warning toast if permission’s already there.
// xx it'll be necessary if we want to Override/swallow system shortcuts like Cmd+Space
object GlobalHotKey {
    private val robot by lazy { java.awt.Robot() }

    fun arm(
        windowState: WindowState,
        frame: ComposeWindow,
        scope: CoroutineScope,
        density: Density,
    ) {
        runCatching { GlobalScreen.registerNativeHook() }
            .onFailure { bounceUserAndDie() }

        GlobalScreen.addNativeKeyListener(object : NativeKeyListener {
            override fun nativeKeyPressed(e: NativeKeyEvent) {
                if ((e.modifiers and NativeKeyEvent.CTRL_MASK) != 0 &&
                    e.keyCode == NativeKeyEvent.VC_SPACE
                ) {
                    onCtrlSpace(frame, windowState, scope, density)
                }
            }
        })
    }

    private fun onCtrlSpace(
        frame: ComposeWindow,
        windowState: WindowState,
        scope: CoroutineScope,
        density: Density,
    ) {
        println("A  hit CTRL-Space  cursorInside=${cursorInside(frame)}  minimized=${frame.isMinimized}")

        /* turn the monitor’s pixel size into dp once */
        val screen = frame.graphicsConfiguration.bounds
        val fullDp = with(density) {
            // 100 % of the current monitor; use *0.8f if   4/5
            DpSize(screen.width.toDp(), screen.height.toDp())
        }

        when {
            cursorInside(frame) && windowState.placement != WindowPlacement.Maximized -> {
//                frame.placement = WindowPlacement.Floating
                windowState.placement = WindowPlacement.Floating
                println("B  launching animateResizeFrame to $fullDp")                //  <-- B

                println("🌊 Alrdy in ! Smooth upsize to full")
            }

            (!frame.isMinimized && frame.isVisible) -> {
                println("🔥 Ctrl-Space: Jumping to window")
                println("B2 jump branch")                                            //  <-- B2

                scope.launch {
                    zoomToFull(frame)
                }

                frame.requestFocus()
                frame.toFront()

//                frame.placement = WindowPlacement.Maximized
//                frame.placement = WindowPlacement.Floating
//                windowState.placement = WindowPlacement.Floating
//                scope.launch(Dispatchers.Main.immediate) {
//                    animateResizeFrame(
//                        frame = frame, windowState = windowState, target = fullDp, density = density, durationMs = 2480
//                    )
//                }

                getDesktop().requestForeground(true)
                warpMouseToCenter(frame)
            }

            frame.isMinimized -> SwingUtilities.invokeLater {
                println("🔥 Ctrl-Space: Un-minimizing window")

                frame.state = Frame.NORMAL      // un-iconify
                frame.isVisible = true          // just in case
                frame.toFront()                 // raise
                frame.requestFocus()            // grab focus
            }
        }
    }

    fun cursorInside(w: ComposeWindow): Boolean {
        val p: java.awt.Point = MouseInfo.getPointerInfo().location
        val rect: Rectangle = w.bounds
        return rect.contains(p)
    }

    private fun bounceUserAndDie() {
        Runtime.getRuntime().exec(
            "open x-apple.systempreferences:com.apple.preference.security?Privacy_ListenEvent"
        )
        println("⚠️ Enable Input Monitoring for this app, then relaunch.")
        exitProcess(1)
    }

    private fun warpMouseToCenter(frame: ComposeWindow) {
        val p = frame.locationOnScreen
        val x = p.x + frame.width / 2
        val y = p.y + frame.height / 2
        robot.mouseMove(x, y)
    }
}
