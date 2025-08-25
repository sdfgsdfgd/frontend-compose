package data

import di.DI
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ui.login.model.AuthState

// region Client 1 - Primarily for use with Github API (with auth handling)
object GithubClient {
    val http: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(AuthHeader)
        install(DefaultRequest) {
            header(HttpHeaders.UserAgent, "Arcana-KMP/0.1")
        }

        /* ─── Auto-logout on 401 ─────────────────────────────────── */
        HttpResponseValidator {
            handleResponseExceptionWithRequest { cause, _ ->
                if (cause is ClientRequestException &&
                    cause.response.status == HttpStatusCode.Unauthorized
                ) {
                    DI.gitRepository.logout()
                }
            }
        }

        expectSuccess = true
    }
}

private val AuthHeader = createClientPlugin("AuthHeader") {
    onRequest { request, _ ->
        request.headers.remove(HttpHeaders.Authorization)

        (DI.gitRepository.state.value as? AuthState.Authenticated)
            ?.token?.accessToken
            ?.let { request.headers.append(HttpHeaders.Authorization, "Bearer $it") }
    }
}
// endregion

// region Client 2 - For general use (no auth handling)
object StandardClient {
    val http = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        expectSuccess = true
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
    }
}
// endregion