// shared/src/desktopMain/kotlin/net/sdfgsdfg/PlatformContext.desktop.kt
package net.sdfgsdfg.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.compose.resources.DrawableResource
import java.net.URI

/**
 * Desktop doesn’t need anything – an empty object is enough.
 */
actual class PlatformContext

actual val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext> =
    staticCompositionLocalOf { PlatformContext() }

@Composable
actual fun DrawableResource.toPlayablePath(): String {
    /*  The generated accessor’s fully‑qualified name is
     *  e.g.  "net.sdfgsdfg.resources.Res$drawable$earth"
     *  → take the bit after the last '$' and append the original file‑suffix.
     */
    val fileName = this::class.simpleName!!          // "earth"
    val path = "drawable/$fileName.mp4"          // you stored earth.mp4

    val url = DrawableResource::class.java.classLoader
        ?.getResource(path)
        ?: error("Resource $path not found in desktop jar")

    return URI(url.toString()).toString()            // VLCJ wants URI‑string
}