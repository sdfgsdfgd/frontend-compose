package net.sdfgsdfg.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import org.jetbrains.compose.resources.DrawableResource

/** A minimal handle to whatever platform context each target needs. */
expect class PlatformContext

/** Ambient access (only if you really need it in Composables). */
expect val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext>


/** Simple wrapper for window metrics. */
data class WindowMetrics(
    val sizeDp: DpSize,
    val pixels: IntSize,
    val density: Float,
)

expect val LocalWindowMetrics: ProvidableCompositionLocal<WindowMetrics>

/** Helper every platform reuses. */
@Composable
expect fun rememberWindowMetrics(): WindowMetrics

@Composable
expect fun Video(
    source: DrawableResource,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
)