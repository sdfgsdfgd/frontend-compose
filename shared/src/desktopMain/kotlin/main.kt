package net.sdfgsdfg

//import antlr.parseKt
//import antlr.parsePython
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Toolkit

fun main() = application {
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val desktopWidth = screenSize.width.dp
    val desktopHeight = screenSize.height.dp

    //xx ==============
//    parseKt()
//    parsePython()

//    demo()
    //xx ==============

    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(desktopWidth * 4 / 5, desktopHeight * 4 / 5)
    )

    Window(
        title = "Arcana - Codebase Comprehension Engine - Desktop App",
        state = windowState,
        alwaysOnTop = false,
        undecorated = true,
        transparent = true,
        onCloseRequest = ::exitApplication,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            SetupWindow(windowState)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SetupWindow(windowState: WindowState) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current

    val desktopWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val desktopHeight = with(density) { windowInfo.containerSize.height.toDp() }

    LaunchedEffect(Unit) {
        windowState.size = DpSize(desktopWidth / 2, desktopHeight / 2)
        windowState.position = WindowPosition(desktopWidth / 2, desktopHeight / 2)
    }

    var dragOffsetX by remember { mutableStateOf(0.dp) }
    var dragOffsetY by remember { mutableStateOf(0.dp) }

    LaunchedEffect(dragOffsetX, dragOffsetY) {
        windowState.position = WindowPosition(
            windowState.position.x + dragOffsetX,
            windowState.position.y + dragOffsetY
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    dragOffsetX += with(density) { dragAmount.x.toDp() }
                    dragOffsetY += with(density) { dragAmount.y.toDp() }
                }
            }
    ) {
        MainApp()
    }
}