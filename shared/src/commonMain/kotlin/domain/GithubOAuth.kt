package domain

import data.model.AccessToken
import data.model.AuthRequest
import data.model.GithubEmail
import data.model.GithubUser
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
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import platform.DeepLinkHandler
import platform.PKCE
import platform.REDIRECT
import platform.STATE_PREFIX
import ui.login.model.AuthState

object GithubOAuth {
    private const val CLIENT_ID = "Ov23libLAx2DZVS5FeM4"
    private const val CLIENT_SECRET = "1b28eb97612c885f785558b82cf92ab033480af4"

    // xx 2nd client because we want custom client w/ oauth headers only for Github related flows
    private val http = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        expectSuccess = true

        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
    }

    fun buildAuthRequest(): AuthRequest {
        val pkce = PKCE.new()
        val state = STATE_PREFIX + buildString {
            repeat(16) { append("0123456789abcdef".random()) }
        }
        val url = URLBuilder("https://github.com/login/oauth/authorize").apply {
            parameters.append("client_id", CLIENT_ID)
            parameters.append("redirect_uri", REDIRECT)
            parameters.append("scope", "repo read:user user:email")
            parameters.append("response_type", "code")
            parameters.append("code_challenge", pkce.challenge)
            parameters.append("code_challenge_method", "S256")
            parameters.append("state", state)
        }.buildString()
        return AuthRequest(url, pkce)
    }

    suspend fun awaitToken(req: AuthRequest): AuthState = runCatching {
        val callback = DeepLinkHandler.uriFlow.first()
        val code = Url(callback).parameters["code"]!!

        val token: AccessToken = http.post("https://github.com/login/oauth/access_token") {
            header(HttpHeaders.Accept, "application/json")
            setBody(FormDataContent(Parameters.build {
                append("client_id", CLIENT_ID)
                append("client_secret", CLIENT_SECRET)
                append("code", code)
                append("code_verifier", req.pkce.value)
            }))                                                           // send form-encoded body
        }.body()

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

        println(
            """
        ┏━ GitHub OAuth Success ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        ┃  access_token     : ${token.accessToken.take(8)}… (len=${token.accessToken.length})
        ┃  token_type       : ${token.tokenType}
        ┣━ Scope ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        ┃  scope            : ${token.scope}
        ┣━ User ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        ┃  login            : ${user.login}
        ┃  id               : ${user.id}
        ┃  name             : ${user.name ?: "—"}
        ┃  avatar_url       : ${user.avatarUrl}
        ┃  primary mail     : ${emails.firstOrNull { it.primary }?.email ?: "—"}
        ┃  secondary mails  : ${emails.filterNot { it.primary }.joinToString(", ") { it.email }}
        ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━""".trimIndent()
        )

        AuthState.Authenticated(token, user, emails)
    }.getOrElse {
        println("GitHub OAuth failed: ${it.message}")
        AuthState.Error(it)
    }
}
