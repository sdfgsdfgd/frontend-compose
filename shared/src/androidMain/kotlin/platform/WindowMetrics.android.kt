package net.sdfgsdfg.platform

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*

actual val LocalWindowMetrics = compositionLocalOf {
    WindowMetrics(DpSize.Zero, IntSize.Zero, 1f)
}

@Composable
actual fun rememberWindowMetrics(): WindowMetrics {
    val cfg     = LocalConfiguration.current
    val density = LocalDensity.current
    val sizeDp  = DpSize(cfg.screenWidthDp.dp, cfg.screenHeightDp.dp)
    val px      = IntSize(
        with(density) { sizeDp.width.toPx() }.toInt(),
        with(density) { sizeDp.height.toPx() }.toInt()
    )
    return remember(cfg, density) { WindowMetrics(sizeDp, px, density.density) }
}