package ui.login

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import data.model.GithubRepoDTO
import data.model.GithubUser
import di.DI
import di.LocalDI
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
import ui.DynamicIslandWithLuxuryInput
import ui.GlassCard
import ui.GlassStyle
import ui.GlassTopBar
import ui.GoldUnicode
import ui.IslandState
import ui.SkeuoButton
import ui.SkeuoText
import ui.login.model.AuthState

@Composable
fun LoginScreen(
    di: DI = LocalDI.current,
    onAuthed: (AuthState.Authenticated) -> Unit = {},
) {
    val ctx = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val authState by di.gitRepository.state.collectAsState()
    val repos by produceState(initialValue = emptyList<GithubRepoDTO>(), authState) {
        val successState = authState as? AuthState.Authenticated

        value = successState
            ?.takeIf { it.token.scope.contains("repo") }
            ?.let {
                di.githubApiClient.listUserRepos().also { fetchedRepos ->
                    println("${GoldUnicode}----------- Fetched ${fetchedRepos.size} repos for user ${it.user.name}\u001B[0m ---")

                    fetchedRepos.forEachIndexed { i, repo ->
                        println("Repo #$i: ${repo.name} (${repo.id})")
                    }
                }
            } ?: emptyList()
    }

    //
    println("----------[ LoginScreen ]---------              authState: [ $authState ]                          busy [ $busy ]                   repos [ ${repos.size} ]")
    //
    LaunchedEffect(Unit) { di.gitRepository.bootstrap() }
    //

    when (authState) {
        is AuthState.Authenticated -> AuthenticatedPane(
            auth = authState as AuthState.Authenticated,
            repos = repos,
            onAuthed = onAuthed
        )

        is AuthState.Unauthenticated ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SkeuoButton(
                    text = "Login with GitHub",
                    textColor = Color.White.copy(alpha = 0.4f), // Color(0xFF191192), // .copy(alpha = 0.6f), // Color(0x33FFFFFF),
                    modifier = Modifier.padding(12.dp).zIndex(12f),
                    cornerRadius = 58.dp,
                    baseTint = Color(0x010101).copy(alpha = 0.4f),
                    sweepTint = Color(0x313131).copy(alpha = 0.2f),
                ) {
                    scope.launch {
                        busy = true // TODO: add loader & anims for state changes

                        di.gitRepository.login { url -> BrowserLauncher.open(url, ctx) }

                        busy = false
                    }
                }
// region --------------[ DEMOS ]----------------------------------------------------------------------------------------------
// xx                 LiquidGlassDemo()    //  <->    [ WIP ]  ( Last milestone uses Call site of Desktop ? ) ( compose last steps not completed yet )
//                    Demo1DynamicIsland()
//                    Demo2DynamicIslandWLuxuryInput()
//                    ButtonCustomShadow(
//                        text = "sdfjgsjsdfsdfdfg",
//                        onClick = {},
//                        modifier = Modifier
//                            .padding(44.dp)
//                            .fillMaxWidth()
//                            .height(64.dp)
//                            .background(Color.Transparent) // Color(0x010101).copy(alpha = 0.4f)),
//                    )
//                    Demo3ColorClouds()
//                    Demo4CustomShadows()
// endregion --------------[ DEMOS ]----------------------------------------------------------------------------------------------
            }

        is AuthState.Error -> TODO()
    }
}

@Composable
private fun AuthenticatedPane(
    auth: AuthState.Authenticated,
    repos: List<GithubRepoDTO>,
    onAuthed: (AuthState.Authenticated) -> Unit
) {
    LaunchedEffect(auth) { onAuthed(auth) }

    // Single source of truth for the search box
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var islandState by rememberSaveable { mutableStateOf<IslandState>(IslandState.Split) }

    // Canonical source list
    val ordered = remember(repos) { repos.sortedByDescending { Instant.parse(it.updatedAt) } }

    // todo: make sure deep-diff works with repo commits, and this triggers with `repos` updates
    // Minimal fuzzy: tokens must each match index OR be contained OR be a subsequence
    val filtered: List<IndexedValue<GithubRepoDTO>> by remember(ordered, query.text) {
        derivedStateOf {
            val tokens = query.text.trim().lowercase()
                .split(Regex("\\s+")).filter { it.isNotEmpty() }

            if (tokens.isEmpty()) ordered.withIndex().toList()
            else ordered.withIndex().filter { (i, r) ->
                val hay = buildString {
                    append(r.name); append(' ')
                    if (!r.description.isNullOrBlank()) append(r.description); append(' ')
                    r.language?.let { append(it) }
                }.lowercase()

                tokens.all { t ->
                    val idxHit = t.all(Char::isDigit) && (i + 1).toString().startsWith(t)
                    idxHit || hay.contains(t) || isSubsequence(t, hay)
                }
            }
        }
    }

    ConstraintLayout(Modifier.fillMaxSize()) {
        val (topBar, bodyLeft, bodyRight) = createRefs()

        GlassTopBar(
            modifier = Modifier.constrainAs(topBar) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            height = 84.dp,
            style = GlassStyle().copy(
                bodyTint = Color.Black,
                bodyFadeStart = 0.56f, bodyFadeMid = 0.76f,
                bloomAlpha = 0.55f, bloomRadiusScale = 4.2f,
                specularAlpha = 0.72f, specularTail = 0.01f,
                innerRimColor = Color.Yellow, rimGap = 1.2.dp,
                alpha1 = 0.8f, alpha2 = 0.8f, alpha3 = 0.9f, alpha4 = 0.85f,
                radius = 32.dp, cornerRadius = 26.dp, rimStroke = 2.8.dp,
                rimBaseAlpha = 0.04f, rimGlowDelta = 0.06f,
            )
        ) {
            ConstraintLayout(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                val (indicator, content) = createRefs()

                Spacer(modifier = Modifier.height(32.dp))
                SkeuoText(
                    modifier = Modifier.constrainAs(content) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                    text = "some other content toooooo",
                    fontSize = 34.sp,
                    textColor = Color.DarkGray/*, modifier = Modifier.align(Alignment.Center)*/
                )
                // TODO:   28 Aug Tue :    0. filter repos fuzzymatch  2. selection anims  3. THEN sync
//                if (isWorkspaceSelected) {
//                    Spacer(Modifier.width(8.dp))
//                    WorkspaceSyncStatus(state = sync, modifier = Modifier.widthIn(max = 400.dp))
//                }

                Spacer(modifier = Modifier.height(8.dp))

                HeartbeatIndicator(modifier = Modifier.constrainAs(indicator) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    bottom.linkTo(parent.bottom)
                })
            }
        }

        /* LEFT: list driven directly by `filtered` */
        LazyColumn(
            modifier = Modifier
                .constrainAs(bodyLeft) {
                    top.linkTo(topBar.bottom, margin = 4.dp)
                    start.linkTo(parent.start, margin = 4.dp)
                    bottom.linkTo(parent.bottom)

                    height = Dimension.fillToConstraints
                }
                .widthIn(max = 620.dp)
                .padding(horizontal = 12.dp)
        ) {
            items(
                items = filtered,
                key = { iv -> repoKey(iv.value) }
            ) { iv ->
                val origIdx = iv.index
                val repo = iv.value

                GlassCard(
                    selected = repo.name.contains("kaangpt", ignoreCase = true),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .animateContentSize()
                        .animateItem(),
                    onClick = { println("repo click: ${repo.name}") }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        SkeuoText(
                            text = "${origIdx + 1}:  ${repo.name}",
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
                            text = " 🫆 ${repo.language ?: "Unknown"}    ✨     ${repo.stars}     ·     ${
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
                            }     ·  ",
                            fontSize = 14.sp,
                            color = Color(0x88B0B0B0),
                            modifier = Modifier.wrapContentWidth().align(Alignment.Start)
                        )
                        // todo-2: color coded   <3h (green)  <1w (yellow)  >1w (red)
                    }
                }
            }
        }

        /* RIGHT: hook the island to the same `query` state */
        Column(
            modifier = Modifier.constrainAs(bodyRight) {
                top.linkTo(topBar.bottom, 12.dp)
                start.linkTo(bodyLeft.end, 4.dp)
                end.linkTo(parent.end, 12.dp)
                bottom.linkTo(parent.bottom, 12.dp)
            },
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) { NeonWelcome(auth.user) }
            Spacer(Modifier.height(8.dp))

            DynamicIslandWithLuxuryInput(
                state = islandState,
                value = query,
                onValueChange = { query = it },
                onSend = { query = TextFieldValue("") } // clear on send
            )

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
                ModeChip("Compact") { islandState = IslandState.Default }
                ModeChip("Split") { islandState = IslandState.Split }
                ModeChip("FaceUnlock") { islandState = IslandState.FaceUnlock }
            }
        }
    }
}

/* tiny helper: forgiving subsequence match, e.g. "krn" hits "KotlinRunner" */
private fun isSubsequence(needle: String, hay: String): Boolean {
    if (needle.isEmpty()) return true
    var i = 0
    for (c in hay) if (i < needle.length && c == needle[i]) i++
    return i == needle.length
}

private fun repoKey(r: GithubRepoDTO): String = "${r.name}|${r.updatedAt}"

@Composable
private fun ModeChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .background(Color(0x22FFFFFF), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) { Text(label, color = Color.White) }
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
