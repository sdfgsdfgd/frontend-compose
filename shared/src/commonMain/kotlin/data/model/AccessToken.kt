package data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import platform.PKCECode

/* ───────────────────────  data  ───────────────────── */
data class AuthRequest(val url: String, val pkce: PKCECode)

/** Token payload that GitHub returns */
@Serializable
data class AccessToken(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    val scope: String
)

/** TODO: Confirm there are more fields */
@Serializable
data class GithubUser(
    val login: String,
    val id: Long,
    val name: String? = null,

    @SerialName("avatar_url")
    val avatarUrl: String,

    val email: String? = null
)

/** TODO: Confirm there are more fields */
@Serializable
data class GithubEmail(
    val email: String,
    val primary: Boolean,
    val verified: Boolean,
    val visibility: String? = null
)
