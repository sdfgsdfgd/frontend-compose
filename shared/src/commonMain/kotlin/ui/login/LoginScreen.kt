package net.sdfgsdfg.ui.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.sdfgsdfg.platform.LocalPlatformContext
import net.sdfgsdfg.ui.SkeuoText
import net.sdfgsdfg.data.AuthManager
import ui.login.BrowserLauncher
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

    // kick off bootstrap once
    LaunchedEffect(Unit) { AuthManager.bootstrap() }
    LaunchedEffect(authState) { println("LoginScreen authState -> $authState") }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (authState) {
            is AuthState.Unauthenticated -> Button(
                enabled = !busy,
                onClick = {
                    scope.launch {
                        busy = true // TODO: add loader & anims for state changes

                        AuthManager.login { url -> BrowserLauncher.open(url, ctx) }

                        busy = false
                    }
                }
            ) { Text(if (busy) "Connecting…" else "Login with GitHub") }

            is AuthState.Error -> TODO()
            is AuthState.Authenticated -> {
                val auth = authState as AuthState.Authenticated

                /* live clock for the skeuomorphic vibe (kotlinx‑datetime, works in commonMain) */
                var now by remember { mutableStateOf(Clock.System.now()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(1_000)
                        now = Clock.System.now()
                    }
                }
                val timeStr = remember(now) {
                    val dt = now.toLocalDateTime(TimeZone.currentSystemDefault()).time
                    dt.toString().take(8)  // HH:mm:ss
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            shape = RoundedCornerShape(58.dp)
                            clip = false
                        }
                        .zIndex(2f)) {

                    /* main welcome line with neon‑ish glow */
                    SkeuoText(
                        text = "Welcome, ${auth.user.name}!",
                        fontSize = 54.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    /* digital clock underneath, lighter glow */
                    SkeuoText(
                        text = timeStr,
                        fontSize = 96.sp
                    )
                }

                /* notify parent exactly once when auth object changes */
                LaunchedEffect(auth) {
                    onAuthed(auth)
                }
            }
        }
    }
}
