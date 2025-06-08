package data

import data.model.GithubRepoDTO
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType

object GithubApi {
    /** Returns first page (40 repos) – TODO: introduce paging, load the rest */
    suspend fun listUserRepos(): List<GithubRepoDTO> = runCatching {
        ApiClient.http.get("https://api.github.com/user/repos") {
            parameter("visibility", "all")
            parameter("per_page", 40)
            accept(ContentType.Application.Json)
        }.body<List<GithubRepoDTO>>()
    }.getOrElse { e ->
        println("Failed to fetch user repos: ${e.message}")

        emptyList()
    }
}
