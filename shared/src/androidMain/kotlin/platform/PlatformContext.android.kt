package net.sdfgsdfg.platform

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.DrawableResource
import androidx.core.net.toUri

/**
 * On Android we just wrap the real `android.content.Context`.
 * You keep the wrapper thin, so it’s cheap.
 */
actual class PlatformContext internal constructor(
    val ctx: Context
)

actual val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext> =
    staticCompositionLocalOf {
        error("No Android Context provided – wrap your Composable tree in " +
                "`CompositionLocalProvider(LocalPlatformContext provides PlatformContext(context))`")
    }

/**
 * Convert a drawable resource into an `android.resource://…` Uri string
 * that Media3 / ExoPlayer understands.
 */
@Composable
actual fun DrawableResource.toPlayablePath(): String {
    val ctx   = LocalContext.current
    val resId = ctx.resources.getIdentifier(
        /* name   */ this::class.simpleName,   // "earth"
        /* defType*/ "drawable",
        /* defPkg */ ctx.packageName
    )
    return "android.resource://${ctx.packageName}/$resId".toUri().toString()
}