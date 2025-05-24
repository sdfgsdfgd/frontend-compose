package ui.login

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import okio.Path
import kotlinx.serialization.json.Json
import ui.login.model.AccessToken
import ui.login.model.AuthRequest
import ui.login.model.AuthState
import ui.login.model.GithubEmail
import ui.login.model.GithubUser
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

expect val REDIRECT: String        // the URI sent to GitHub
expect val STATE_PREFIX: String    // "", "mob-", etc.

object GithubOAuth {
    private const val CLIENT_ID = "Ov23libLAx2DZVS5FeM4"
    private const val CLIENT_SECRET = "1b28eb97612c885f785558b82cf92ab033480af4"

    private val http = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        expectSuccess = true

        install(Logging) {
            logger = Logger.SIMPLE
            level  = LogLevel.ALL
        }
    }

    /* ───────────────────────  Step-0 : URL build  ─────────────────────── */
    fun buildAuthRequest(): AuthRequest {
        val pkce = PKCE.new()
        val state = STATE_PREFIX + buildString {
            repeat(16) { append("0123456789abcdef".random()) }
        }
        val url = URLBuilder("https://github.com/login/oauth/authorize").apply {
            parameters.append("client_id", CLIENT_ID)
            parameters.append("redirect_uri", REDIRECT)
            parameters.append("scope", "read:user user:email")
            parameters.append("response_type", "code")
            parameters.append("code_challenge", pkce.challenge)
            parameters.append("code_challenge_method", "S256")
            parameters.append("state", state)
        }.buildString()
        return AuthRequest(url, pkce)
    }

    /* ───────────────────────  Step-1 : token + user  ───────────────────── */
    suspend fun awaitToken(req: AuthRequest): AuthState = runCatching {
        val callback = DeepLinkHandler.uriFlow.first()
        val code     = Url(callback).parameters["code"]!!

        val token: AccessToken = http.post("https://github.com/login/oauth/access_token") {
            header(HttpHeaders.Accept, "application/json")
            setBody(FormDataContent(Parameters.build {
                append("client_id",     CLIENT_ID)
                append("client_secret", CLIENT_SECRET)
                append("code",          code)
                append("code_verifier", req.pkce.value)
            }))                                                           // send form-encoded body
        }.body()


        /* -> optional e-mail / user info grab right away */
        val user: GithubUser = http.get("https://api.github.com/user") {
            header(HttpHeaders.Authorization, "Bearer ${token.accessToken}")
            accept(ContentType.Application.Json)
        }.body()

        val emails: List<GithubEmail> = runCatching {
            http.get("https://api.github.com/user/emails") {
                header(HttpHeaders.Authorization, "Bearer ${token.accessToken}")
                accept(ContentType.Application.Json)
            }.body<List<GithubEmail>>()
        }.getOrElse { emptyList() }

        /* -------- pretty log block -------- */
        println(
            """
        ┏━ GitHub OAuth Success ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        ┃  access_token : ${token.accessToken.take(8)}… (len=${token.accessToken.length})
        ┃  token_type   : ${token.tokenType}
        ┃  scope        : ${token.scope}
        ┣━ User ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        ┃  login        : ${user.login}
        ┃  id           : ${user.id}
        ┃  name         : ${user.name ?: "—"}
        ┃  primary mail : ${emails.firstOrNull { it.primary }?.email ?: "—"}
        ┃  avatar_url   : ${user.avatarUrl}
        ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        """.trimIndent()
        )

        AuthState.Authenticated(token, user, emails)
    }.getOrElse { AuthState.Error(it) }
}

// xx CORNER OF SINGLE QUICK PLATFORM IMPLEMENTATIONS
expect fun appDataPath(fileName: String, platformCtx: Any): Path

// BrowserLauncher:  Interface to open a URL in the system's default browser
expect object BrowserLauncher {
    fun open(url: String, platformCtx: Any)
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