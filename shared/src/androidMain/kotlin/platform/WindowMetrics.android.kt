package net.sdfgsdfg.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

actual val LocalWindowMetrics = compositionLocalOf {
    WindowMetrics(DpSize.Zero, IntSize.Zero, 1f)
}

@Composable
actual fun rememberWindowMetrics(): WindowMetrics {
    val win = LocalWindowInfo.current
    val cfg = LocalConfiguration.current
    val density = LocalDensity.current

    val sizeDp = DpSize(win.containerSize.width.dp, win.containerSize.height.dp)
    val px = IntSize(
        with(density) { sizeDp.width.toPx() }.toInt(),
        with(density) { sizeDp.height.toPx() }.toInt()
    )
    return remember(cfg, density) { WindowMetrics(sizeDp, px, density.density) }
}