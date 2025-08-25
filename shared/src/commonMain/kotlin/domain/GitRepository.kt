package domain

import data.model.AccessToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ui.login.model.AuthState

class GitRepository(
    private val dataStore: DataStore,
    private val oauth: GithubOAuth,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _state = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val state: StateFlow<AuthState> = _state

    fun bootstrap() = scope.launch {
        dataStore.load()?.let { cache ->
            println("---------------------------------\n\nAuto-login as ${cache.user.name} – token cached, ${cache.emails.size} emails \n")
            println("---[ Scope:  ${cache.scope} ]---\n\n---------------------------------")

            _state.value = AuthState.Authenticated(
                token = AccessToken(accessToken = cache.token, tokenType = "bearer", scope = cache.scope),
                user = cache.user,
                emails = cache.emails
            )
        }
    }

    suspend fun login(onAuthUrl: (String) -> Unit) {
        val req = oauth.buildAuthRequest()

        onAuthUrl(req.url)

        when (val res = oauth.awaitToken(req)) {
            is AuthState.Error -> _state.value = AuthState.Error(res.cause)
            is AuthState.Authenticated -> {
                dataStore.save(res.token, res.user, res.emails)
                _state.value = AuthState.Authenticated(res.token, res.user, res.emails)
            }

            is AuthState.Unauthenticated -> {}
        }
    }

    fun logout() = scope.launch {
        dataStore.clear()
        _state.value = AuthState.Unauthenticated
    }
}
