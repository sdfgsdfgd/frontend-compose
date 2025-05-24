package net.sdfgsdfg.ui.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import net.sdfgsdfg.platform.LocalPlatformContext
import ui.login.AuthManager
import ui.login.BrowserLauncher
import ui.login.GithubOAuth
import ui.login.model.AuthState

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onAuthed: (AuthState.Authenticated) -> Unit = {}
) {
    val ctx = LocalPlatformContext.current

    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    val authState by AuthManager.state.collectAsState()

    LaunchedEffect(Unit) { AuthManager.bootstrap() }

    println("== ===[ LoginScreen ]=== === authState: [ $authState ] === ===")

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(
            enabled = !busy,
            onClick = {
                scope.launch {
                    busy = true

                    // STEP 2: open in platform browser
                    val req = GithubOAuth.buildAuthRequest()
                    AuthManager.login { url -> BrowserLauncher.open(req.url, ctx) }

//                    BrowserLauncher.open(req.url, ctx)

                    // STEP 3: wait for redirect & token
                    when (val res = GithubOAuth.awaitToken(req)) {
                        is AuthState.Authenticated -> {
                            println(">>> logged in as ${res.user.login}")
                            onAuthed(res)
                        }
                        is AuthState.Error   -> {
                            println("Auth err: ${res.cause}")
                        }
                        AuthState.Unauthenticated -> println("Auth cancelled")
                    }
                    busy = false
                }
            }
        ) { Text(if (busy) "Connecting…" else "Login with GitHub") }
    }
}
