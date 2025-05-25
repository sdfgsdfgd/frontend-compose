// ui/login/model/GithubRepo.kt
package ui.login.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRepo(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val private: Boolean,
    val description: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("stargazers_count") val stars: Int,
    val language: String? = null,
    @SerialName("updated_at") val updatedAt: String
)
