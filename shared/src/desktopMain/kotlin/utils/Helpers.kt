package net.sdfgsdfg.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.sdfgsdfg.resources.Res
import net.sdfgsdfg.resources.allDrawableResources
import org.jetbrains.compose.resources.DrawableResource
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.awt.Component
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private val cache = ConcurrentHashMap<DrawableResource, String>()

// Why does this exist ?
//  💥 VLCJ only understands “real” file/URI paths.
//  💥 It cannot open the virtual entries that live inside your JAR (like “drawable/earth.mp4”)
fun DrawableResource.toPlayablePath(): String = cache.getOrPut(this) {
    val key = Res.allDrawableResources.entries.first { it.value === this }.key

    // copy once to a tmp file; return its *plain* path
    File.createTempFile(key, ".mp4").apply {
        deleteOnExit()
        runBlocking(Dispatchers.IO) {
            writeBytes(Res.readBytes("drawable/$key.mp4"))
        }
    }.absolutePath
}

/* helper so both components share the same call‐site */
internal fun Component.mediaPlayer(): MediaPlayer = when (this) {
    is CallbackMediaPlayerComponent -> mediaPlayer()
    is EmbeddedMediaPlayerComponent -> mediaPlayer()
    else -> error("Unexpected VLCJ component")
}

fun isMacOS() = System.getProperty("os.name")?.lowercase()?.contains(Regex("mac|darwin")) ?: false