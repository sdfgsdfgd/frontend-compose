// WindowMetrics.desktop.kt
package net.sdfgsdfg.platform

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*

actual val LocalWindowMetrics = compositionLocalOf {
    WindowMetrics(DpSize.Zero, IntSize.Zero, 1f)
}

@Composable
actual fun rememberWindowMetrics(): WindowMetrics {
    val info    = LocalWindowInfo.current
    val density = LocalDensity.current
    val sizeDp  = with(density) {
        DpSize(info.containerSize.width.toDp(), info.containerSize.height.toDp())
    }
    return remember(info, density) {
        WindowMetrics(sizeDp, info.containerSize, density.density)
    }
}