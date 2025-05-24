package ui.login

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import okio.Path

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

actual fun appDataPath(fileName: String, platformCtx: Any): Path {
    TODO("Not yet implemented")
}