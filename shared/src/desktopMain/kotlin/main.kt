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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import net.sdfgsdfg.platform.LocalWindowMetrics
import net.sdfgsdfg.platform.rememberWindowMetrics
import java.awt.Toolkit

fun main() = application {
    System.setProperty("compose.interop.blending", "true")
    /* screen-size calc unchanged */
    val screen = Toolkit.getDefaultToolkit().screenSize
    val deskSize = DpSize(screen.width.dp * 4 / 5, screen.height.dp * 4 / 5)
    val windowState = rememberWindowState(size = deskSize, position = WindowPosition(Alignment.Center))

    Window(
        title = "Arcana – Desktop",
        state = windowState,
        undecorated = true,
        transparent = true,
        alwaysOnTop = false,
        onCloseRequest = ::exitApplication
    ) {
        /* provide metrics for MainScreen */
        CompositionLocalProvider(
            LocalWindowMetrics provides rememberWindowMetrics()
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


// xx OLD CODE
// xx OLD CODE
// xx OLD CODE
// xx OLD CODE
// xx OLD CODE
// xx OLD CODE
// xx OLD CODE
//fun main2() = application {
//    val screenSize = Toolkit.getDefaultToolkit().screenSize
//    val desktopWidth = screenSize.width.dp
//    val desktopHeight = screenSize.height.dp
//
//    //xx ==============
////    parseKt()
////    parsePython()
//
////    demo()
//    //xx ==============
//
//    val windowState = rememberWindowState(
//        position = WindowPosition(Alignment.Center),
//        size = DpSize(desktopWidth * 4 / 5, desktopHeight * 4 / 5)
//    )
//
//    Window(
//        title = "Arcana - Codebase Comprehension Engine - Desktop App",
//        state = windowState,
//        alwaysOnTop = false,
//        undecorated = true,
//        transparent = true,
//        onCloseRequest = ::exitApplication,
//    ) {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//        ) {
//            SetupWindow(windowState)
//        }
//    }
//}
//
//@OptIn(ExperimentalComposeUiApi::class)
//@Composable
//fun SetupWindow(windowState: WindowState) {
//    val windowInfo = LocalWindowInfo.current
//    val density = LocalDensity.current
//
//    val desktopWidth = with(density) { windowInfo.containerSize.width.toDp() }
//    val desktopHeight = with(density) { windowInfo.containerSize.height.toDp() }
//
//    LaunchedEffect(Unit) {
//        windowState.size = DpSize(desktopWidth / 2, desktopHeight / 2)
//        windowState.position = WindowPosition(desktopWidth / 2, desktopHeight / 2)
//    }
//
//    var dragOffsetX by remember { mutableStateOf(0.dp) }
//    var dragOffsetY by remember { mutableStateOf(0.dp) }
//
//    LaunchedEffect(dragOffsetX, dragOffsetY) {
//        windowState.position = WindowPosition(
//            windowState.position.x + dragOffsetX,
//            windowState.position.y + dragOffsetY
//        )
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .pointerInput(Unit) {
//                detectDragGestures { _, dragAmount ->
//                    dragOffsetX += with(density) { dragAmount.x.toDp() }
//                    dragOffsetY += with(density) { dragAmount.y.toDp() }
//                }
//            }
//    ) {
//        MainApp()
//    }
//}
// xx OLD CODE
// xx OLD CODE
// xx OLD CODE
// xx OLD CODE
// xx OLD CODE
