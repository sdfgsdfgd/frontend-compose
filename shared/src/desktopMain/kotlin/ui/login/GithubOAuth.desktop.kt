package ui.login

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.uri
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okio.Path
import okio.Path.Companion.toPath
import java.awt.Desktop.getDesktop
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths

actual val REDIRECT = "http://localhost:1410/callback"
actual val STATE_PREFIX = ""          // no prefix needed

actual object BrowserLauncher {
    actual fun open(url: String, platformCtx: Any) {
        // 1) start one-shot server (port 1410)
        embeddedServer(CIO, host = "127.0.0.1", port = 1410) {
            routing {
                get("/callback") {
                    call.respondText("✔ Login complete — you may close this tab.")
                    DeepLinkHandler.emit(call.request.uri)
                    launch { delay(300); this@embeddedServer.dispose() }
                }
            }
        }.start(false)

        runCatching { getDesktop().browse(URI(url)) }
            .onFailure {
                Runtime.getRuntime().exec(arrayOf("xdg-open", url))
                println("Failed to open browser: ${it.message}")
            }.onSuccess {
                println("Opened browser to: $url")
            }
    }
}

actual fun sha256(bytes: ByteArray): ByteArray = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)

actual object DeepLinkHandler {
    private val _flow = MutableSharedFlow<String>(replay = 1)
    actual val uriFlow = _flow.asSharedFlow()
    internal fun emit(uri: String) {
        _flow.tryEmit(uri)
    }
}

actual fun appDataPath(fileName: String, platformCtx: Any): Path {
    val base: String = when {
        System.getProperty("os.name").startsWith("Windows", ignoreCase = true) ->
            System.getenv("APPDATA") + "\\Arcana"                     // e.g. C:\Users\Kaan\AppData\Roaming\Arcana
        else -> System.getProperty("user.home") + "/.arcana"         // macOS & Linux: ~/.arcana
    }
    val dir = Paths.get(base)
    Files.createDirectories(dir)                                     // ensure exists
    return "$base/$fileName".toPath()
}