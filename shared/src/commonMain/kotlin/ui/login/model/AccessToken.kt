package ui.login.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ui.login.PKCE

/* ───────────────────────  data  ───────────────────── */
data class AuthRequest(val url: String, val pkce: PKCE.Verifier)

/** Full token payload GitHub returns */
@Serializable
data class AccessToken(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type")   val tokenType: String,
    val scope: String
)

/** Minimal user record (expand as you need) */
@Serializable
data class GithubUser(
    val login: String,
    val id: Long,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String,
    val email: String? = null
)

/** E-mail records (GitHub may return many) */
@Serializable
data class GithubEmail(
    val email: String,
    val primary: Boolean,
    val verified: Boolean,
    val visibility: String? = null
)

/* ─────────── AuthResult sealed type ─────────── */
sealed interface AuthState {
    data class Authenticated(
        val token: AccessToken,
        val user: GithubUser,
        val emails: List<GithubEmail>
    ) : AuthState

    data object Unauthenticated : AuthState
    data class Error(val cause: Throwable) : AuthState
}
