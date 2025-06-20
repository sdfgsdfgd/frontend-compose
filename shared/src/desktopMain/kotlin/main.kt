package net.sdfgsdfg

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.sdfgsdfg.resources.Res
import platform.GlobalHotKey
import platform.LocalPlatformContext
import platform.LocalWindowMetrics
import platform.PlatformContext
import platform.rememberWindowMetrics
import ui.MainScreen
import java.awt.Desktop
import java.awt.EventQueue
import java.awt.Frame
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.desktop.QuitEvent
import java.awt.desktop.QuitResponse
import java.net.URI
import javax.swing.SwingUtilities
import kotlin.math.roundToInt
import kotlin.system.exitProcess

private var winRef: ComposeWindow? = null

fun main() = application {
    System.setProperty("compose.interop.blending", "true")

    val screenState = Toolkit.getDefaultToolkit().screenSize
    val windowState = rememberWindowState(
        size = DpSize(screenState.width.dp * 4 / 5, screenState.height.dp * 4 / 5),
        position = WindowPosition(Alignment.Center)
    )

    /* ── cancel any Quit request and just iconify ───────────── */
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().setQuitHandler { _: QuitEvent, resp: QuitResponse ->
            // TODO: do like ChatGPT instead, also removes from Process list ? so only way to bring back is Top-bar --> Open Chatgpt window   which creates it again ?
            winRef?.state = Frame.ICONIFIED      // dock-ify
            resp.cancelQuit()                    // JVM stays alive
        }
    }

    Window(
        title = "Arcana – Desktop",
        state = windowState,
        undecorated = true,
        transparent = true,
        alwaysOnTop = false,
        onCloseRequest = {},
    ) {
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current

        LaunchedEffect(Unit) {
            winRef = window
            GlobalHotKey.arm(windowState, window, scope, density)
            installTrayHook(window)
        }

        CompositionLocalProvider(
            LocalPlatformContext provides PlatformContext(),
            LocalWindowMetrics provides rememberWindowMetrics(),
        ) {
            DraggableWindow(windowState) {
                MainScreen(
                    metrics = LocalWindowMetrics.current,
                    autoPlay = true
                )
            }
        }
    }
}

// xx ==========================================
fun installTrayHook(window: ComposeWindow) {
    if (!SystemTray.isSupported()) return                // Wayland / head-less guard

    /* ── 1. turn Res-URI → java.awt.Image ─────────────────────────────── */
    val image = Toolkit.getDefaultToolkit().getImage(
        URI(Res.getUri("drawable/tray_bot_icon.png")).toURL()
    )

    /* ── 2. build the tray icon ───────────────────────────────────────── */
    val trayIcon = TrayIcon(image, "Arcana").apply {
        isImageAutoSize = true
        addActionListener { window.isVisible = !window.isVisible }
        popupMenu = PopupMenu().apply {
            add(MenuItem("Show / Hide").apply {
                addActionListener { window.isVisible = !window.isVisible }
            })
            addSeparator()
            add(MenuItem("Quit").apply {
                addActionListener { exitProcess(0) }
            })
        }
    }

    /* ── 3. add to the AWT SystemTray on the EDT ──────────────────────── */
    SwingUtilities.invokeLater {
        SystemTray.getSystemTray().add(trayIcon)
    }
}

suspend fun zoomToFull(
    window: ComposeWindow,
    durationMs: Int = 1200
) = coroutineScope {                        // ← gives us a scope for `launch`

    val startX = window.x
    val startY = window.y
    val startW = window.width
    val startH = window.height
    val screen = window.graphicsConfiguration.bounds

    println("DEBUG-ZOOM  start($startX,$startY $startW×$startH)  " +
            "target(0,0 ${screen.width}×${screen.height})")

    val ax = Animatable(startX.toFloat())
    val ay = Animatable(startY.toFloat())
    val aw = Animatable(startW.toFloat())
    val ah = Animatable(startH.toFloat())
    val spec = tween<Float>(durationMs, easing = FastOutSlowInEasing)

    val jobs = listOf(
        launch { ax.animateTo(0f, spec) },
        launch { ay.animateTo(0f, spec) },
        launch { aw.animateTo(screen.width .toFloat(),  spec) },
        launch { ah.animateTo(screen.height.toFloat(), spec) }
    )

    var tick = 0
    // inside zoomToFull’s while–loop ─ ONE place to patch
    do {
        val x = ax.value.roundToInt()
        val y = ay.value.roundToInt()
        val w = aw.value.roundToInt()
        val h = ah.value.roundToInt()

        runOnEDTBlocking {                         // ← use the safe helper
            window.setBounds(x, y, w, h)
            // dummy.repaint()           // keep disabled unless you really need it
            println("frame ${"%03d".format(++tick)}  $w×$h")
        }

        delay(16)
    } while (isActive && (aw.isRunning || ah.isRunning))
    jobs.forEach { it.join() }

    println("DEBUG-ZOOM  done, frames=$tick")
}

inline fun runOnEDTBlocking(crossinline body: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) {
        // Already on the EDT → just run it.
        body()
    } else {
        EventQueue.invokeAndWait { body() }    // Jump to EDT and wait.
    }
}

// xx ==========================================

@Composable
private fun DraggableWindow(
    state: WindowState,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var dragX by remember { mutableStateOf(0.dp) }
    var dragY by remember { mutableStateOf(0.dp) }

    LaunchedEffect(dragX, dragY) {
        state.position = WindowPosition(state.position.x + dragX, state.position.y + dragY)
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { _, drag ->
                    dragX += with(density) { drag.x.toDp() }
                    dragY += with(density) { drag.y.toDp() }
                }
            }
    ) { content() }
}
