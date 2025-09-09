package ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
import kotlin.math.abs

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

    LaunchedEffect(Unit) { di.gitRepository.bootstrap() }
    println("----------[ LoginScreen ]---------              authState: [ $authState ]                          busy [ $busy ]                   repos [ ${repos.size} ]")

    when (authState) {
        is AuthState.Authenticated -> AuthenticatedPane(
            auth = authState as AuthState.Authenticated,
            repos = repos,
            onAuthed = onAuthed
        )

        is AuthState.Unauthenticated ->
            // TODO-1: Extract to an UnauthenticatedPane()
            // TODO-2: animated transitions between states, crossfade or other anims/easings, maybe even Runtimeshader
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

@OptIn(DelicateCoroutinesApi::class)
@Composable
private fun AuthenticatedPane(
    auth: AuthState.Authenticated,
    repos: List<GithubRepoDTO>,
    onAuthed: (AuthState.Authenticated) -> Unit
) {
    // <-- WS Client -->
    val ws = LocalDI.current.websocketClient

    // <-- INPUT -->
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }

    var islandState by rememberSaveable { mutableStateOf<IslandState>(IslandState.Default) }
    val listState = rememberLazyListState() // for scrolling control
    val focusRequester = remember { FocusRequester() }
    val ordered = remember(repos) { repos.sortedByDescending { Instant.parse(it.updatedAt) } } // Repos sorted by updatedAt descending

    // todo: make sure deep-diff works with repo commits, and this triggers with `repos` updates
    // Minimal fuzzy: tokens must each match index OR be contained OR be a subsequence
    val reposFiltered: List<IndexedValue<GithubRepoDTO>> by remember(ordered, query.text) {
        derivedStateOf {
            val tokens = query.text.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (tokens.isEmpty())
                ordered.withIndex().toList()
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

    val onAuthedState by rememberUpdatedState(onAuthed)
    // Job handle for backend selection flow; declared early for lifecycle hooks
    var selectJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(Unit) {
        onAuthedState(auth)
        focusRequester.requestFocus() // Auto-focis
    }

    DisposableEffect(Unit) {
        onDispose { selectJob?.cancel() }
    }

    // Keyboard navigation and selection state per query persistence
    val selectionMap = remember { mutableMapOf<Int, String>() }
    val queryQueue = remember { ArrayDeque<Int>() }
    val queryHash = query.text.hashCode()

    var selectedKey by remember(queryHash, reposFiltered) {
        mutableStateOf(
            selectionMap[queryHash]?.takeIf { savedKey ->
                reposFiltered.any { repoKey(it.value) == savedKey }
            } ?: reposFiltered.firstOrNull()?.let { repoKey(it.value) }.orEmpty()
        )
    }
    LaunchedEffect(selectedKey, queryHash) {
        if (selectedKey.isNotEmpty()) {
            selectionMap[queryHash] = selectedKey

            queryQueue.remove(queryHash); queryQueue.addLast(queryHash)

            while (queryQueue.size > 20) {
                val eldest = queryQueue.removeFirst()
                selectionMap.remove(eldest)
            }
        }
    }

    var scrollAnimationJob by remember { mutableStateOf<Job?>(null) }

    // Workspace selection + sync UI state
    var isWorkspaceSelected by remember { mutableStateOf(false) }
    var sync by remember { mutableStateOf(SyncUiState()) }

    // TODO:  1.  Optimise beyond-viewport rapid UP/DOWN nav, make it scrollby faster instead of animating item into view
    //        2.  Make scroll animation smarter, predictive, interruptible
    //             ( Interruption, velocity continuum via a central thread of control )
    LaunchedEffect(selectedKey) {
        val target = reposFiltered.indexOfFirst { repoKey(it.value) == selectedKey }
        if (target < 0) return@LaunchedEffect

        // wait until we actually have something laid out
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }

        scrollAnimationJob?.cancelAndJoin()
        scrollAnimationJob = launch {
            fun visibleRange(): IntRange = listState.layoutInfo.visibleItemsInfo.let {
                (it.firstOrNull()?.index ?: 0)..(it.lastOrNull()?.index ?: 0)
            }

            fun avgVisibleSize(): Float = listState.layoutInfo.let { li ->
                val v = li.visibleItemsInfo
                val vp = (li.viewportEndOffset - li.viewportStartOffset).toFloat()
                (v.map { it.size }.average().toFloat()).takeIf { it > 0f }
                    ?: (vp / v.size.coerceAtLeast(1))
            }

            fun centerDelta(idx: Int): Float? = listState.layoutInfo.let { li ->
                val item = li.visibleItemsInfo.firstOrNull { it.index == idx } ?: return@let null
                val vpCenter = (li.viewportStartOffset + li.viewportEndOffset) / 2f
                item.offset + item.size / 2f - vpCenter
            }

            val range0 = visibleRange()
            if (target !in range0) {
                val goingDown = target > range0.last
                val itemsDelta = if (goingDown) target - range0.last + 0.5f
                else range0.first - target + 0.5f
                val dir = if (goingDown) +1 else -1
                listState.animateScrollBy(
                    dir * avgVisibleSize() * itemsDelta,
                    tween(800, easing = FastOutSlowInEasing)
                    // spring( stiffness = 420f,dampingRatio = DampingRatioLowBouncy, visibilityThreshold = 1f)
                )
            }

            centerDelta(target)?.takeIf { abs(it) > 2f }?.let { delta ->
                listState.animateScrollBy(
                    delta,
                    tween(600, easing = FastOutSlowInEasing)
                    // swap to spring(...) for the plush kiss
                    // spring(stiffness = 40f,dampingRatio = DampingRatioHighBouncy,visibilityThreshold = 1f)
                )
            }
        }
    }

    ConstraintLayout(
        Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && reposFiltered.isNotEmpty()) {
                    when (keyEvent.key) {
                        Key.DirectionUp, Key.DirectionDown -> {
                            val currentIndex = reposFiltered.indexOfFirst { repoKey(it.value) == selectedKey }
                            val newIndex = when (keyEvent.key) {
                                Key.DirectionUp -> (currentIndex - 1).coerceAtLeast(0)
                                Key.DirectionDown -> (currentIndex + 1).coerceAtMost(reposFiltered.size - 1)
                                else -> currentIndex
                            }
                            if (newIndex != currentIndex) {
                                selectedKey = repoKey(reposFiltered[newIndex].value)
                            }
                            true
                        }
                        Key.Enter, Key.NumPadEnter -> {
                            if (!isWorkspaceSelected) {
                                reposFiltered.firstOrNull { repoKey(it.value) == selectedKey && !isWorkspaceSelected }?.value?.let { selectedRepo ->
                                    isWorkspaceSelected = true
                                    // Seed initial sync state – backend wiring can update this later
                                    sync = SyncUiState(status = SyncStatus.Initializing, progress = 0, message = "Preparing workspace…")
                                    println("ENTER: selected repo ${selectedRepo.name} (${selectedRepo.id})")

                                    // Kick off backend selection flow with logging and UI updates
                                    selectJob?.cancel()
                                    val (owner, name) = selectedRepo.fullName.split('/').let {
                                        (it.getOrNull(0) ?: "") to (it.getOrNull(1) ?: selectedRepo.name)
                                    }
                                    val repoData = GitHubRepoData(
                                        repoId = selectedRepo.id,
                                        name = name,
                                        owner = owner,
                                        url = selectedRepo.htmlUrl,
                                        branch = null
                                    )
                                    val token = auth.token.accessToken

                                    selectJob = GlobalScope.launch {
                                        ws.selectRepoFlow(repoData, token).collect { r ->
                                            val p = (r.progress ?: 0).coerceIn(0, 100)
                                            sync = when (r.status.lowercase()) {
                                                "error" -> SyncUiState(SyncStatus.Error(r.message), p, r.message)
                                                "success" -> SyncUiState(SyncStatus.Synchronized, 100, r.message ?: "Synchronized ✨")
                                                "cloning" -> {
                                                    val st = if (p < 10) SyncStatus.Initializing else SyncStatus.Syncing
                                                    SyncUiState(st, p, r.message)
                                                }

                                                else -> sync
                                            }
                                        }
                                    }
                                }
                                true
                            } else false
                        }
                        Key.Escape -> {
                            if (isWorkspaceSelected) {
                                isWorkspaceSelected = false
                                selectJob?.cancel()
                                println("ESC: returning to repo list")
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
    ) {
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
                    text = "Select a workspace to continue",
                    fontSize = 34.sp,
                    textColor = Color.DarkGray/*, modifier = Modifier.align(Alignment.Center)*/
                )

                // TODO:   28-30 Aug:
                //          0. filter repos fuzzymatch  [DONE]
                //          1. selection anims  [DONE]
                //                 - - ^ done ^ - -
                //                        --
                //          2.  ENTER --> select repo --> Workspace details --> Workspace Sync Composable
                //          3. Messaging ( around this time we Dolphin dive back into the Engine as well.
                //                          Benchmark case, engine completion for kotlin-codebases, browsi capability etc.. )
                //
                //
                AnimatedVisibility(visible = isWorkspaceSelected, enter = fadeIn(tween(660)), exit = fadeOut(tween(220))) {
                    Spacer(Modifier.width(8.dp))
                    WorkspaceSyncStatus(state = sync, modifier = Modifier.widthIn(max = 400.dp))
                }
                // === === === === === === TODO === === === === === === //

                Spacer(modifier = Modifier.height(8.dp))

                HeartbeatIndicator(modifier = Modifier.constrainAs(indicator) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    bottom.linkTo(parent.bottom)
                })
            }
        }

        /* LEFT: list driven directly by `filtered` with fade-out on selection */
        AnimatedVisibility(
            visible = !isWorkspaceSelected,
            modifier = Modifier
                .constrainAs(bodyLeft) {
                    top.linkTo(topBar.bottom, margin = 4.dp)
                    start.linkTo(parent.start, margin = 4.dp)
                    bottom.linkTo(parent.bottom)

                    height = Dimension.fillToConstraints
                }
                .widthIn(max = 620.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(240))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = reposFiltered,
                    key = { iv -> repoKey(iv.value) }
                ) { iv ->
                    val origIdx = iv.index
                    val repo = iv.value
                    val isSelected = repoKey(repo) == selectedKey

                    GlassCard(
                        selected = isSelected,
                        modifier = Modifier
                            .padding(horizontal = if (isSelected) 8.dp else 5.dp, vertical = if (isSelected) 8.dp else 4.dp)
                            .animateContentSize()
                            .animateItem(
                                fadeInSpec = tween(1400, easing = FastOutSlowInEasing),
                                fadeOutSpec = tween(100, easing = FastOutSlowInEasing),
                                placementSpec = tween(676, easing = FastOutSlowInEasing)
                            ),
                        onClick = {
                            selectedKey = repoKey(repo)
                            println("repo click: ${repo.name}")
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            SkeuoText(
                                text = "${origIdx + 1}:  ${repo.name}",
                                fontSize = 34.sp,
                                textColor = Color.Yellow.copy(alpha = 0.75f),
                                modifier = Modifier.wrapContentWidth().align(Alignment.CenterHorizontally)
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
                                    Instant.parse(repo.updatedAt)
                                        .toLocalDateTime(TimeZone.currentSystemDefault())
                                        .format(
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
                        }
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
