package net.sdfgsdfg.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import net.sdfgsdfg.utils.isMacOS
import net.sdfgsdfg.utils.mediaPlayer
import net.sdfgsdfg.utils.toPlayablePath
import org.jetbrains.compose.resources.DrawableResource
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.InputEvents
import java.awt.Component


/**
 * Desktop doesn’t need anything – an empty object is enough.
 */
actual class PlatformContext

actual val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext> =
    staticCompositionLocalOf { PlatformContext() }


actual val LocalWindowMetrics = compositionLocalOf {
    WindowMetrics(DpSize.Zero, IntSize.Zero, 1f)
}

@Composable
actual fun rememberWindowMetrics(): WindowMetrics {
    val info = LocalWindowInfo.current
    val density = LocalDensity.current
    val sizeDp = with(density) {
        DpSize(info.containerSize.width.toDp(), info.containerSize.height.toDp())
    }
    return remember(info, density) {
        WindowMetrics(sizeDp, info.containerSize, density.density)
    }
}

@Composable
actual fun Video(
    source: DrawableResource,
    modifier: Modifier,
    autoPlay: Boolean
) {
    val url = remember { source.toPlayablePath() }
    val comp = remember { initializeMediaPlayerComponent() }
    val mp = remember { comp.mediaPlayer() }

    LaunchedEffect(url, autoPlay) {
        println("Playing video: $url")

        mp.controls().repeat = true
        mp.media().play(url, ":input-repeat=65535")   // loop forever

        if (!autoPlay) mp.controls().pause()
    }

    SwingPanel(factory = { comp }, modifier = modifier)
}


/**
 * See https://github.com/caprica/vlcj/issues/887#issuecomment-503288294
 * for why we're using CallbackMediaPlayerComponent for macOS.
 */
fun initializeMediaPlayerComponent(): Component {
    NativeDiscovery().discover()
    return if (isMacOS()) {
        CallbackMediaPlayerComponent(
            MediaPlayerFactory("--no-play-and-pause"), null,
            InputEvents.NONE, true, null, null, null, null,
        )
    } else {
        EmbeddedMediaPlayerComponent("--no-play-and-pause")
    }
}


