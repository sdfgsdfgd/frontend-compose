package ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

@Composable
actual fun MetaContainer(modifier: androidx.compose.ui.Modifier, cutoff: Float, content: @Composable BoxScope.() -> Unit) {
}

actual fun DrawScope.drawCombinedGradientStroke(
    path: Path,
    sweepBrush: Brush,
    strokeWidthPx: Float,
    shapeSize: Size,
    shape: Shape
) {
}

actual fun DrawScope.customShadow(
    shadow: Shadow,
    shape: Shape,
    size: Size,
    layoutDirection: LayoutDirection,
    isInner: Boolean,
) {
}

@Composable
actual fun androidx.compose.ui.Modifier.blurEffect(radius: Dp): androidx.compose.ui.Modifier = this

//
// ──────────────────────────────────────────────────────────────────────
// region Liquid Glass
@Composable
actual fun androidx.compose.ui.Modifier.liquidGlass(state: LiquidGlassProviderState, style: LiquidGlassStyle): androidx.compose.ui.Modifier = this

actual object DesktopCaptureBridge {
    init {
        System.loadLibrary("DesktopCapture")
    }

    actual external fun hasScreenCapturePermission(): Boolean
    actual external fun startCapture(callback: FrameCallback)
    actual external fun createSkiaImageFromIOSurface(surfacePtr: Long, contextPtr: Long): Long
    actual external fun createImageBitmapFromSkiaImage(skImagePtr: Long): ImageBitmap
}
// endregion
// ──────────────────────────────────────────────────────────────────────
//

actual object TimeMark {
    actual fun nanoTime() = System.nanoTime()
}
