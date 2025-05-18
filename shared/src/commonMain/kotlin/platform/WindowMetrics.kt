// shared/src/commonMain/kotlin/net/sdfgsdfg/WindowMetrics.kt
package net.sdfgsdfg.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.unit.*

data class WindowMetrics(
    val sizeDp : DpSize,
    val pixels : IntSize,
    val density: Float
)

expect val LocalWindowMetrics: ProvidableCompositionLocal<WindowMetrics>

/** helper every platform re-uses */
@Composable
expect fun rememberWindowMetrics(): WindowMetrics