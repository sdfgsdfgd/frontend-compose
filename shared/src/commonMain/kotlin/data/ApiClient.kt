package data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ui.login.AuthManager
import ui.login.model.AuthState

object ApiClient {

    /** Single, shared Ktor client */
    val http = HttpClient(CIO) {

        install(ContentNegotiation) { json(Json) }

        /* ─── Inject bearer dynamically on EVERY request ─────────── */
        install(DefaultRequest) {
            header(HttpHeaders.Authorization) {
                when (val st = AuthManager.state.value) {
                    is AuthState.Authenticated -> "Bearer ${st.token.accessToken}"
                    else                        -> null          // header omitted
                }
            }
        }

        /* ─── Auto-logout on 401 ─────────────────────────────────── */
        HttpResponseValidator {
            handleResponseExceptionWithRequest { cause, _ ->
                if (cause is ClientRequestException &&
                    cause.response.status == HttpStatusCode.Unauthorized
                ) {
                    AuthManager.logout()
                }
            }
        }

        expectSuccess = true
    }
}