package ui.login

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ui.login.model.AccessToken
import ui.login.model.AuthState
import ui.login.model.GithubEmail

object AuthManager {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _state = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val state: StateFlow<AuthState> = _state

    /** Call once from your Application/compose main */
    fun bootstrap() = scope.launch {
        TokenStore.load()?.let { (token, user, emails) ->
            println("Auto-login as ${user.name} - token: $token - emails: ${emails.size} primary-email: ${emails.first()}")
            _state.value = AuthState.Authenticated(
                token = AccessToken(token, "bearer", "cached"), // type alias if you like
                user = user,
                emails = listOf()
            )
        }
    }

    suspend fun login(onAuthUrl: (String) -> Unit) {
        val req = GithubOAuth.buildAuthRequest()
        onAuthUrl(req.url)                       // let UI open browser
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


