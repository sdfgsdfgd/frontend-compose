package data

import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.*
import ui.login.model.GithubRepo

object GithubApi {
    /** Returns first page (30 repos) – tweak per_page / page as needed. */
    suspend fun listUserRepos(): List<GithubRepo> =
        ApiClient.http.get("https://api.github.com/user/repos") {
            parameter("visibility", "all")
            parameter("per_page", 30)
            accept(ContentType.Application.Json)
        }.body()
}
