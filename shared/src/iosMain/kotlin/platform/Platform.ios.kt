package platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import net.sdfgsdfg.resources.Res
import net.sdfgsdfg.resources.allDrawableResources
import okio.Path
import org.jetbrains.compose.resources.DrawableResource
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.play
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.UIKit.UIScreen
import platform.UIKit.UIView

/* ─────────── window metrics ─────────── */

private val LocalMetrics = staticCompositionLocalOf {
    WindowMetrics(DpSize.Zero, IntSize.Zero, 1f)
}

actual val LocalWindowMetrics: ProvidableCompositionLocal<WindowMetrics> = LocalMetrics

@OptIn(ExperimentalForeignApi::class)
@Composable
fun rememberWindowMetrics(): WindowMetrics {
    val density = LocalDensity.current
    val bounds = UIScreen.mainScreen.bounds
    val pixels = bounds.useContents {
        IntSize(size.width.toInt(), size.height.toInt())
    }
    val sizeDp = with(density) { DpSize(pixels.width.toDp(), pixels.height.toDp()) }
    return remember { WindowMetrics(sizeDp, pixels, density.density) }
}

/* ─────────── platform context ─────────── */

actual class PlatformContext

actual val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext> =
    staticCompositionLocalOf { PlatformContext() }

/* ─────────────────────  video helper ─────────────────────── */
fun DrawableResource.toPlayablePath(): String {
    val key = Res.allDrawableResources.entries.first { it.value === this }.key
    return Res.getUri("drawable/$key.mp4")
}

/* ─────────── Video composable ─────────── */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun Video(
    source: DrawableResource,
    modifier: Modifier,
    autoPlay: Boolean,
) {
    // turn the logical drawable into a URI that AVPlayer understands
    val uri = remember(source) {
        val key = Res.allDrawableResources
            .entries.first { it.value === source }.key          // "earth"
        Res.getUri("drawable/$key.mp4")                         // “…/earth.mp4”
    }

    // UIKit ↔︎ Compose bridge (from compose-interop-uikit)
    UIKitView(
        modifier = modifier,
        factory  = {
            /* AVPlayer → AVPlayerLayer → UIView */
            val player = AVPlayer(uRL = NSURL(string = uri))
            val layer  = AVPlayerLayer.playerLayerWithPlayer(player)

            // UIView(frame: CGRectZero)
            val view = UIView(CGRectZero.readValue())
            layer.frame = view.bounds
            view.layer.addSublayer(layer)

            if (autoPlay) player.play()
            view                                            // ← return the UIView
        }
    )
}

actual object BrowserLauncher {
    actual fun open(url: String, platformCtx: Any) {}
}

actual fun sha256(bytes: ByteArray): ByteArray {
    TODO("Not yet implemented")
}

actual object DeepLinkHandler {
    private val ch = Channel<String>(Channel.CONFLATED)
    actual val uriFlow: SharedFlow<String> = TODO() /*ch.receiveAsFlow().stateIn( */
}

actual val REDIRECT: String
    get() = TODO("Not yet implemented")
actual val STATE_PREFIX: String
    get() = TODO("Not yet implemented")

actual object AppDirs {
    actual val path: Path
        get() = TODO("Not yet implemented")
    actual fun init(platformCtx: Any?) {
    }
}