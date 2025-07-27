package ui.login

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import data.GithubApi
import data.model.GithubRepoDTO
import data.model.GithubUser
import domain.AuthManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import platform.BrowserLauncher
import platform.LocalPlatformContext
import ui.ButtonCustomShadow
import ui.ColorCloudDEMO
import ui.FluidMetaBallContainer
import ui.GlassCard
import ui.GlassStyle
import ui.GlassTopBar
import ui.GoldUnicode
import ui.SkeuoButton
import ui.SkeuoText
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
    var repos by remember { mutableStateOf<List<GithubRepoDTO>>(emptyList()) }

    LaunchedEffect(Unit) { AuthManager.bootstrap() }
    LaunchedEffect(authState) {
        println("LoginScreen authState -> $authState")
        val successState = (authState as? AuthState.Authenticated)

        if (successState?.token?.scope?.contains("repo") == true) {
            repos = GithubApi.listUserRepos().also {
                println("${GoldUnicode}----------- Fetched ${it.size} repos for user ${successState.user.name}\u001B[0m ---")

                it.forEachIndexed { i, repo ->
                    println("Repo #$i: ${repo.name} (${repo.id})")
                }
            }
        }
    }

    Box(modifier.fillMaxSize().padding(top = 184.dp), contentAlignment = Alignment.TopCenter) { // Center) {
        when (authState) {
            is AuthState.Unauthenticated ->
                Column {
                    SkeuoButton(
                        text = "Login with GitHub",
                        textColor = Color.White.copy(alpha = 0.4f), // Color(0xFF191192), // .copy(alpha = 0.6f), // Color(0x33FFFFFF), // .copy(alpha = .65f),
                        modifier = Modifier.padding(12.dp).zIndex(12f),
                        cornerRadius = 58.dp,
                        baseTint = Color(0x010101).copy(alpha = 0.4f),
                        sweepTint = Color(0x313131).copy(alpha = 0.2f),
                    ) {
                        scope.launch {
                            busy = true // TODO: add loader & anims for state changes

                            AuthManager.login { url -> BrowserLauncher.open(url, ctx) }

                            busy = false
                        }
                    }

                    // xx LiquidGlassDemo()    //  <->    [ WIP ]  ( Last milestone uses Call site of Desktop ? ) ( compose last steps not completed yet )

                    //
                    //
                    //
                    //
                    // xx ========= Dynamic Island Work =============
                    var split by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        FluidMetaBallContainer(
                            isSplit = split,
                            blurRadius = 6.dp,
                            cutoff = 0.8f,
                            islandSize = DpSize(220.dp, 48.dp),
                            bubbleSize = DpSize(60.dp, 48.dp),
                            splitOffset = 40.dp,
                            islandContent = { Text("🌴", fontSize = 20.sp, color = Color.White) },
                            bubbleContent = { Text("⏳", fontSize = 20.sp, color = Color.White) }
                        )

                        Spacer(Modifier.height(40.dp))

                        Button(onClick = { split = !split }) {
                            Text(if (split) "Merge" else "Split")
                        }
                    }

                    // xx ========= Dynamic Island Work =============
                    //
                    //
                    //
                    //
                    //
                    //

                    ButtonCustomShadow(
                        text = "sdfjgsjsdfsdfdfg",
                        onClick = {},
                        modifier = Modifier
                            .padding(44.dp)
//                            .zIndex(12f)
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(Color.Transparent) // Color(0x010101).copy(alpha = 0.4f)),
                    )
                    ColorCloudDEMO()
                    // InnerAndOuterShadowDEMO()
                }

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
    repos: List<GithubRepoDTO>,
    onAuthed: (AuthState.Authenticated) -> Unit
) {
    LaunchedEffect(auth) { onAuthed(auth) } // notify parent once
    // todo: make sure deep-diff works with repo commits, and this triggers with `repos` updates
    val ordered = remember(repos) { repos.sortedByDescending { Instant.parse(it.updatedAt) } }

    ConstraintLayout {
        val (topBar, body) = createRefs()

        Box(Modifier.fillMaxWidth().constrainAs(topBar) {
            top.linkTo(parent.top)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        }) {
            GlassTopBar(
                84.dp, style = GlassStyle().copy(
                    bodyTint = Color.Black,
                    bodyFadeStart = 0.56f, bodyFadeMid = 0.76f,
                    bloomAlpha = 0.55f, bloomRadiusScale = 4.2f,
                    specularAlpha = 0.72f, specularTail = 0.01f,
                    innerRimColor = Color.Yellow, rimGap = 1.2.dp,
                    alpha1 = 0.8f, alpha2 = 0.8f, alpha3 = 0.9f, alpha4 = 0.85f,
                    radius = 32.dp, cornerRadius = 26.dp, rimStroke = 2.8.dp, rimBaseAlpha = 0.04f, rimGlowDelta = 0.06f,
                )
            ) {
                Column(verticalArrangement = Arrangement.Center) {
                    Spacer(modifier = Modifier.height(32.dp))

                    SkeuoText(text = "some other content toooooo", fontSize = 34.sp, textColor = Color.DarkGray/*, modifier = Modifier.align(Alignment.Center)*/)

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Row(Modifier.fillMaxSize().constrainAs(body) {
            top.linkTo(topBar.bottom)
            start.linkTo(parent.start, margin = 12.dp)
            end.linkTo(parent.end, margin = 12.dp)
        }
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 620.dp)
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp)
//                    .padding(top = 32.dp)
            ) {
                itemsIndexed(items = ordered) { i, repo ->
                    GlassCard(
                        selected = repo.name.contains("kaangpt"),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .animateContentSize()
                            .animateItem(),
                        onClick = { println("yallah yallah") }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            SkeuoText(
                                text = "${i + 1}:  ${repo.name} ",
                                fontSize = 34.sp,
                                textColor = Color.Yellow.copy(alpha = 0.75f), // Color(0xddFFD966), // Color.Yellow,
                                modifier = Modifier.wrapContentWidth().align(Alignment.CenterHorizontally)//.align(Alignment.Top)
                            )
                            if (repo.description?.isNotBlank() == true) Spacer(Modifier.height(16.dp))
                            SkeuoText(
                                repo.description.orEmpty(),
                                fontSize = 18.sp,
                                textColor = Color(0xee898989),
                                modifier = Modifier.wrapContentWidth().align(Alignment.Start)
                            )
                            if (repo.description?.isNotBlank() == true) Spacer(Modifier.height(4.dp))
                            Text(
                                text = " 🫆 ${repo.language ?: "Unknown"}   ✨   ${repo.stars}     · · ⏰ · ·       ${
                                    Instant.parse(repo.updatedAt)                              // ISO string → Instant
                                        .toLocalDateTime(TimeZone.currentSystemDefault())      // local time
                                        .format(                                               // pretty -> “4 May 2025 · 16:54”
                                            LocalDateTime.Format {
                                                dayOfMonth(); char(' ')
                                                monthName(MonthNames.ENGLISH_ABBREVIATED); char(' ')
                                                year(); chars(" · ")
                                                hour(); char(':'); minute()
                                            }
                                        )
                                }     · · ⏰ · ·",
                                fontSize = 14.sp,
                                color = Color(0x88B0B0B0),
                                modifier = Modifier.wrapContentWidth().align(Alignment.Start)
                            )
                            // todo-2: color coded   <3h (green)  <1w (yellow)  >1w (red)
                        }
                    }
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
