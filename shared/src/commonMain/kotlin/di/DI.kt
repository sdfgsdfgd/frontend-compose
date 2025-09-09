package di

import androidx.compose.runtime.compositionLocalOf
import data.GithubApi
import data.GithubClient
import data.StandardClient
import domain.DataStore
import domain.git.GitRepository
import domain.git.GithubOAuth
import ui.login.WsClient

// --------[ Dependency Injection ]-------- //
val LocalDI = compositionLocalOf { DI }

/**
 * 🌳 DI Tree – Dependency Injection Root
 *
 * Dependencies are structured clearly from leaves (🌿) upward.
 */
object DI {
    // --------[ Leaves 🌿 ]-------- //
    private val dataStore = DataStore

    private val githubHttpClient = GithubClient.http
    private val httpClient = StandardClient.http

    private val githubOAuth = GithubOAuth(httpClient)

    // --------[ API 🍀 ]-------- //
    val githubApiClient = GithubApi(githubHttpClient)

    // --------[ Repo ]-------- //
    val gitRepository = GitRepository(
        dataStore = dataStore,
        oauth = githubOAuth
    )

    val websocketClient = WsClient(
        client = httpClient,
        json = StandardClient.J
    )
}
