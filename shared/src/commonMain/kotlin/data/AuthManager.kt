package net.sdfgsdfg.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ui.login.GithubOAuth
import ui.login.TokenStore
import ui.login.model.AccessToken
import ui.login.model.AuthState

object AuthManager {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _state = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val state: StateFlow<AuthState> = _state

    /** Call once from your Application/compose main */
    fun bootstrap() = scope.launch {
        TokenStore.load()?.let { cache ->
            println("---------------------------------\n\nAuto-login as ${cache.user.name} – token cached, ${cache.emails.size} emails \n")
            println("---[ Scope:  ${cache.scope} ]---\n\n---------------------------------")

            _state.value = AuthState.Authenticated(
                token   = AccessToken(accessToken = cache.token, tokenType = "bearer", scope = cache.scope),
                user    = cache.user,
                emails  = cache.emails
            )
        }
    }

    suspend fun login(onAuthUrl: (String) -> Unit) {
        val req = GithubOAuth.buildAuthRequest()

        onAuthUrl(req.url)

        when (val res = GithubOAuth.awaitToken(req)) {
            is AuthState.Error -> _state.value = AuthState.Error(res.cause)
            is AuthState.Authenticated -> {
                TokenStore.save(res.token, res.user, res.emails)
                _state.value = AuthState.Authenticated(res.token, res.user, res.emails)
            }

            AuthState.Unauthenticated -> Unit
        }
    }

    fun logout() = scope.launch {
        TokenStore.clear()
        _state.value = AuthState.Unauthenticated
    }
}


