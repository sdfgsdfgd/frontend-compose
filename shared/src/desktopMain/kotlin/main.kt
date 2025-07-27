package net.sdfgsdfg

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
import platform.GlobalHotKey
import platform.LocalPlatformContext
import platform.LocalWindowMetrics
import platform.PlatformContext
import platform.installTrayHook
import platform.rememberWindowMetrics
import ui.MainScreen
import java.awt.Desktop
import java.awt.Frame
import java.awt.Toolkit
import java.awt.desktop.QuitEvent
import java.awt.desktop.QuitResponse

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
                // xx  LiquidGlassDemoDesktop()   // [ 27.07.05 ]  Use this callsite instead of LoginScreen() of commonMain
                MainScreen(
                    metrics = LocalWindowMetrics.current,
                    autoPlay = true
                )
            }
        }
    }
}

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
