package ui.login

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import net.sdfgsdfg.platform.LocalPlatformContext
import okio.Path
import okio.Path.Companion.toPath

actual val REDIRECT      = "https://sdfgsdfg.net/api/auth/callback/github"
actual val STATE_PREFIX  = "mob-"

actual object BrowserLauncher {
    actual fun open(url: String, platformCtx: Any) {
        val ctx = platformCtx as Context
        CustomTabsIntent.Builder()
            .build()
            .launchUrl(ctx, url.toUri())
    }
}

actual fun sha256(bytes: ByteArray): ByteArray = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)

actual object DeepLinkHandler {
    private val ch = Channel<String>(capacity = Channel.CONFLATED)
    @OptIn(DelicateCoroutinesApi::class)
    actual val uriFlow: SharedFlow<String> = ch.receiveAsFlow().shareIn(
        scope = kotlinx.coroutines.GlobalScope,
        started = kotlinx.coroutines.flow.SharingStarted.Lazily,
        replay = 1
    )
    fun onNewUri(uri: String) {
        ch.trySend(uri)
    }
}

actual fun appDataPath(fileName: String, platformCtx: Any): Path {
    val ctx = platformCtx as Context
    val dir = ctx.getDir("arcana", Context.MODE_PRIVATE)             // /data/data/<pkg>/files/arcana
    return "${dir.absolutePath}/$fileName".toPath()
}