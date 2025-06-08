package platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.SharedFlow
import okio.Path
import org.jetbrains.compose.resources.DrawableResource
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// TODO:  QUESTION / ai check if these are really necessary for desktop, android targets, otherwise we keep local copies there, more minimal
//       isnt there already a stdlib-provided multiplatform way to retrieve ctx already via `LocalContext.current` or something

/** whatever platform context each target needs */
expect class PlatformContext

expect val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext>

data class WindowMetrics(
    val sizeDp: DpSize,
    val pixels: IntSize,
    val density: Float,
)

expect val LocalWindowMetrics: ProvidableCompositionLocal<WindowMetrics>

/** Helper every platform reuses */
@Composable
expect fun rememberWindowMetrics(): WindowMetrics

@Composable
expect fun Video(
    source: DrawableResource,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
)

expect val REDIRECT: String        // the URI sent to GitHub
expect val STATE_PREFIX: String    // "", "mob-", etc.

// BrowserLauncher:  Interface to open a URL in the system's default browser
expect object BrowserLauncher {
    fun open(url: String, platformCtx: Any)
}

expect object AppDirs {
    /** Call once per process (noop on desktop) */
    fun init(platformCtx: Any? = null)

    /** Absolute, per-user, per-app directory – valid after `init` */
    val path: Path
}

// region Crypto / Helpers.
// PKCE (Proof Key for Code Exchange) is a security measure to prevent authorization code interception attacks.
object PKCE {
    private const val ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"

    data class Verifier(val value: String, val challenge: String)

    fun new(): Verifier {
        val verifier = buildString(64) { repeat(64) { append(ALPHA.random()) } }

        val challenge = sha256(verifier.encodeToByteArray()).base64Url()

        return Verifier(verifier, challenge)
    }
}

//////////////////////////////////////////////////////////////////
// Helpers – all commonMain, JVM-free
//////////////////////////////////////////////////////////////////
expect fun sha256(bytes: ByteArray): ByteArray          // ← actuals per target

@OptIn(ExperimentalEncodingApi::class)
private fun ByteArray.base64Url(): String =
    Base64.UrlSafe.encode(this)
// endregion

// region Deep Link Handler
expect object DeepLinkHandler {
    /** Emit full redirect URI when our custom scheme hits. */
    val uriFlow: SharedFlow<String>
}

// endregion