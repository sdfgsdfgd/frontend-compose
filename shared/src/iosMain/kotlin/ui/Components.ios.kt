package ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

@Composable
actual fun MetaContainer(modifier: Modifier, cutoff: Float, content: @Composable BoxScope.() -> Unit) {
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
    customShadow: CustomShadow,
    shape: Shape,
    size: Size,
    layoutDirection: LayoutDirection,
    isInner: Boolean,
) {
}

@Composable
actual fun Modifier.blurEffect(radius: Dp): Modifier {
    TODO("Not yet implemented")
}


@Composable
actual fun Modifier.liquidGlass(state: LiquidGlassProviderState, style: LiquidGlassStyle): Modifier {
    TODO("Not yet implemented")
}

actual object DesktopCaptureBridge {
//    init { System.loadLibrary("DesktopCapture") }

    actual external fun hasScreenCapturePermission(): Boolean
    actual external fun startCapture(callback: FrameCallback)
    actual external fun createSkiaImageFromIOSurface(surfacePtr: Long, contextPtr: Long): Long
    actual external fun createImageBitmapFromSkiaImage(skImagePtr: Long): ImageBitmap
}

actual object TimeMark {
    actual fun nanoTime(): Long {
        TODO("Not yet implemented")
    }
}
