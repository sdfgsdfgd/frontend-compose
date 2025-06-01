package net.sdfgsdfg.ui.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import data.GithubApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.sdfgsdfg.data.AuthManager
import net.sdfgsdfg.platform.LocalPlatformContext
import net.sdfgsdfg.ui.SkeuoText
import net.sdfgsdfg.ui.SkeuoButton
import ui.login.BrowserLauncher
import ui.login.model.AuthState
import ui.login.model.GithubRepo
import ui.login.model.GithubUser

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onAuthed: (AuthState.Authenticated) -> Unit = {}
) {
    val ctx = LocalPlatformContext.current

    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    val authState by AuthManager.state.collectAsState()
    var repos by remember { mutableStateOf<List<GithubRepo>>(emptyList()) }

    // kick off bootstrap once
    LaunchedEffect(Unit) { AuthManager.bootstrap() }
    LaunchedEffect(authState) {
        println("LoginScreen authState -> $authState")
        val successState = (authState as? AuthState.Authenticated)

        if (successState?.token?.scope?.contains("repo") == true) {
            repos = runCatching { GithubApi.listUserRepos() }
                .getOrElse {
                    println("Failed to fetch repos: $it")
                    emptyList()
                }.also {
                    println("----------------------------------------\n\nFetched ${it.size} repos for user ${successState.user.name} \n")
                    it.forEachIndexed { i, repo ->
                        println("Repo #$i: ${repo.name} (${repo.id})")
                    }
                }
        }
    }

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
            is AuthState.Authenticated -> AuthenticatedPane(
                auth = authState as AuthState.Authenticated,
                repos = repos,
                onAuthed = onAuthed
            )
        }
    }
}

@Composable
private fun AuthenticatedPane(
    auth: AuthState.Authenticated,
    repos: List<GithubRepo>,
    onAuthed: (AuthState.Authenticated) -> Unit
) {
    LaunchedEffect(auth) { onAuthed(auth) } // notify parent once

    ConstraintLayout {
        val (topConstrain, bottom) = createRefs()

        Row(Modifier.fillMaxSize().constrainAs(topConstrain) {
            top.linkTo(parent.top)
            start.linkTo(parent.start, margin = 12.dp)
            end.linkTo(parent.end, margin = 12.dp)
        }
        ) {
            LazyColumn(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {
                itemsIndexed(items = repos) { i, repo ->
                    Text(
                        text = repo.name,
                        style = MaterialTheme.typography.body2,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }

            /* ── RIGHT: welcome card ── */
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                NeonWelcome(auth.user)
            }
        }

        SkeuoButton(
            text = "hi how are you",
            modifier = Modifier
                .constrainAs(bottom) {
                    top.linkTo(topConstrain.bottom)
                    start.linkTo(parent.start, margin = 12.dp)
                }
                .padding(12.dp)
                .zIndex(12f),
            cornerRadius = 58.dp,
        ) {
            println("Magic button clicked!")
        }
    }
}

@Composable
private fun NeonWelcome(user: GithubUser) {
    val timeStr by produceState(initialValue = "--:--:--") {
        while (true) {
            value = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .time.toString()
                .take(8)           // HH:mm:ss
            delay(1_000)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SkeuoText("Welcome, ${user.name}!", fontSize = 54.sp)
        Spacer(Modifier.height(12.dp))
        SkeuoText(timeStr, fontSize = 96.sp)
    }
}

@Composable
private fun ErrorPane(err: Throwable) =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Auth failed: ${err.message}")
    }
