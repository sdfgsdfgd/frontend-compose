package ui.login.model

import data.model.AccessToken
import data.model.GithubEmail
import data.model.GithubUser

/* ─────────── Auth State ─────────── */
sealed interface AuthState {
    data class Authenticated(
        val token: AccessToken,
        val user: GithubUser,
        val emails: List<GithubEmail>
    ) : AuthState

    data object Unauthenticated : AuthState
    data class Error(val cause: Throwable) : AuthState
}