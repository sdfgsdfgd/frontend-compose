package platform

import android.content.Context
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.sdfgsdfg.resources.Res
import net.sdfgsdfg.resources.allDrawableResources
import org.jetbrains.compose.resources.DrawableResource
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 *   On Android we just wrap the real `android.content.Context`.
 *    keep the wrapper thin, so it’s cheap.
 */
actual class PlatformContext internal constructor(
    val ctx: Context
)

actual val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext> = staticCompositionLocalOf {
    error(
        "No Android Context provided – wrap your Composable tree in " +
                "`CompositionLocalProvider(LocalPlatformContext provides PlatformContext(context))`"
    )
}

private val cache = ConcurrentHashMap<DrawableResource, String>()

fun DrawableResource.toPlayablePath(): String = cache.getOrPut(this) {
    val key = Res.allDrawableResources.entries.first { it.value === this }.key

    File.createTempFile(key, ".mp4").apply {
        deleteOnExit()
        runBlocking(Dispatchers.IO) {
            writeBytes(Res.readBytes("drawable/$key.mp4"))
        }
    }.absolutePath
}
