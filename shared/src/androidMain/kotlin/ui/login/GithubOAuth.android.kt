package ui.login

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import okio.Path
import okio.Path.Companion.toPath

actual val REDIRECT      = "https://sdfgsdfg.net/api/auth/callback/github"
actual val STATE_PREFIX  = "mob-"

actual object BrowserLauncher {
    actual fun open(url: String, platformCtx: Any) {
        val ctx: Context = when (platformCtx) {
            is Context -> platformCtx
            is net.sdfgsdfg.platform.PlatformContext -> platformCtx.ctx
            else -> error("Unsupported platform context: $platformCtx")
        }

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

actual object AppDirs {
    private lateinit var internal: Path

    /** MUST be called from Application.onCreate() */
    actual fun init(platformCtx: Any?) {
        val ctx = platformCtx as Context
        val dir = ctx.getDir("arcana", Context.MODE_PRIVATE)
        internal = dir.absolutePath.toPath()
    }

    actual val path: Path
        get() = if (::internal.isInitialized) internal
        else error("AppDirs.init(ctx) not called yet")
}