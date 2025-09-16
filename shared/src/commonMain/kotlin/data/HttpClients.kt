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
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ui.login.model.AuthState
import kotlin.time.Duration.Companion.seconds

// -------------[ Client 1 - Primarily for use with Github API (with auth handling) ]---------------------
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

//                          -----------------------------------------------
//               ---------------------------------------------------------------------
//      -----------------------------[ Client 2 - GeneraL ]----------------------------------
//               ---------------------------------------------------------------------
//                          -----------------------------------------------
object StandardClient {
    val J = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    val http = HttpClient(CIO) {
        install(ContentNegotiation) { json(J) }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
            pingInterval = 10.seconds           // custom conn ctrl
            // maxFrameSize = 4L * 1024 * 1024  // optional safety net
        }
        expectSuccess = true
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
    }
}
