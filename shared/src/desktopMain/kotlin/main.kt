package net.sdfgsdfg

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import di.DI
import di.LocalDI
import platform.GlobalHotKey
import platform.LocalPlatformContext
import platform.LocalWindowMetrics
import platform.PlatformContext
import platform.installTrayHook
import platform.rememberIsWindowMoving
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
    // Helps Swing/Compose interop scenarios; documented in JB docs/issues
    // (no effect on Linux; safe on macOS/Windows)
    System.setProperty("sun.awt.noerasebackground", "true")
    System.setProperty("sun.awt.erasebackgroundonresize", "false")
    System.setProperty("skiko.renderApi", "METAL") // Keep using Metal on macOS; default anyway
    System.setProperty("SKIKO_CLEAR_COLOR", "0x00000000")

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

        val isMoving by rememberIsWindowMoving(windowState, debounceMs = 300)

        CompositionLocalProvider(
            LocalPlatformContext provides PlatformContext(),
            LocalWindowMetrics provides rememberWindowMetrics(),
            LocalDI provides DI,
        ) {
            WindowDraggableArea(
                Modifier.fillMaxWidth()
            ) {
                MainScreen(
                    metrics = LocalWindowMetrics.current,
                    autoPlay = true,
                    isWindowMoving = isMoving
                )
            }
        }
    }
}
