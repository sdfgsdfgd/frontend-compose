package ui.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import di.LocalDI
import ui.GlassStyle
import ui.glass
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ui.login.model.ws.ContainerMessage
import ui.login.model.ws.ContainerResponse
import ui.login.model.ws.GitHubRepoData
import ui.login.model.ws.GitHubRepoSelectMessage
import ui.login.model.ws.GitHubRepoSelectResponse
import ui.login.model.ws.ServerEvent
import ui.login.model.ws.SyncStatus
import ui.login.model.ws.SyncUiState
import ui.login.model.ws.WsMessage
import java.util.UUID
import kotlin.math.min
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Composable
fun WorkspaceSyncStatus(
    state: SyncUiState,
    modifier: Modifier = Modifier,
    minHeight: Dp = 44.dp,
    corner: Dp = 14.dp
) {
    LaunchedEffect(state.status, state.progress, state.message) {
        println("[WS-UI] status=${state.status} progress=${state.progress} msg=${state.message}")
    }
    val widthFraction by animateFloatAsState(
        targetValue = state.progress.coerceIn(0, 100) / 100f,
        animationSpec = spring(stiffness = 200f, dampingRatio = 0.78f),
        label = "progressWidth"
    )
    val tint by animateColorAsState(
        targetValue = scrimColorFor(state.status),
        animationSpec = tween(durationMillis = 180),
        label = "tint"
    )

    // Crossfade the label to avoid jump cuts
    val label = statusLabel(state)

    Box(
        modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(RoundedCornerShape(corner))
            .background(Color.Transparent)
            .progressScrim(widthFraction, tint)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = state.progress / 100f,
                    range = 0f..1f,
                    steps = 0
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = label,
            transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
            label = "statusText"
        ) { text: String ->
            Text(
                text = text,
                fontSize = 13.sp,
                color = Color(0xFFE6E6E6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ----- colors -----
private fun scrimColorFor(status: SyncStatus): Color = when (status) {
    is SyncStatus.Error -> Color(0xFF7F1D1D)    // red-900
    SyncStatus.Synchronized -> Color(0xFF14532D) // green-900
    else -> Color(0xFF1E3A8A)                   // blue-900
}.copy(
    alpha = when (status) {
        is SyncStatus.Error -> 0.30f
        SyncStatus.Synchronized -> 0.20f
        else -> 0.20f
    }
)

// ----- modifier: draw the left-to-right tint like your absolute inset overlay -----
fun Modifier.progressScrim(fraction: Float, color: Color) = drawBehind {
    val w = size.width * fraction.coerceIn(0f, 1f)
    if (w > 0f) drawRect(color = color, size = Size(w, size.height))
}

// ----- label helpers -----
@Composable
private fun statusLabel(state: SyncUiState): String = when (state.status) {
    SyncStatus.Initializing -> state.message ?: "Initializing ${if (state.progress > 0) "${state.progress}%" else "..."}"
    SyncStatus.Syncing -> state.message ?: "Syncing Repository ${state.progress}%"
    SyncStatus.Synchronized -> state.message ?: "Synchronized ✨"
    is SyncStatus.Error -> state.message ?: state.status.reason ?: "Sync Error"
}

// ============================================================================= MODELS ===========================================


enum class ConnectionState { Disconnected, Connecting, Connected }

// TODO-1: Lifecycle + reconnect. WsClient connects once and dies quietly on error. No retry/backoff, no window‑close cleanup.
class WsClient(
    private val baseurl: String = "ws://sdfgsdfg.net/ws",
    private val client: HttpClient,
    private val json: Json,
) {
    // ----- Dumb logging for now  TODO: Switch to a global log  (push to local storage/DB + Cloud + param to remoteLog optionally, cloud only logs important )
    private val logEnabled = true
    private fun log(tag: String, msg: String) {
        if (!logEnabled) return
        println("[ WS - $tag ] $msg")
    }

    // TODO:  DI a global scope/dispatcher, inject shared scopes
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(ConnectionState.Disconnected)
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<ServerEvent>(extraBufferCapacity = 512, onBufferOverflow = DROP_OLDEST)
    val events = _events.asSharedFlow()

    private val _latency = MutableStateFlow<Long?>(null)
    val latency = _latency.asStateFlow()

    @Volatile
    private var session: DefaultClientWebSocketSession? = null

    private var reconnectJob: Job? = null

    @Volatile
    private var backoffMs = 500L // grows to 3_600_000L (1h)

    init {
        scope.launch { connectWithRetry() }
        scope.launch {
            state.collect { connState ->
                log("STATE", connState.toString())

                if (connState == ConnectionState.Connected)
                    launchHeartbeat()
            }
        }
    }

    private fun CoroutineScope.launchHeartbeat() {
        launch { latency.filterNotNull().collect { println("🚀 RTT: ${it}ms") } }
        launch {
            events.collect { event ->
                when (event) {
                    is ServerEvent.Repo -> println("[ 📦 ServerEvent.Repo ] ${event.value}")
                    is ServerEvent.Container -> println("[ 📥 ServerEvent.Container ] ${event.value}")
                    is ServerEvent.Pong -> println("🏓 Pong received")
                    is ServerEvent.Raw -> println("📄 Raw Message: ${event.json}")
                    is ServerEvent.Closed -> println("🔴 Connection closed: ${event.reason}")
                }
            }
        }
    }

    private fun launchReader(ws: DefaultClientWebSocketSession) = scope.launch {
        runCatching {
            ws.incoming.consumeAsFlow().collect { frame ->
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        log("launchReader()", "text ${text.length}B: $text${if (text.length > 400) "…" else ""}")
                        parseReceived(text)
                    }

                    is Frame.Binary -> log("launchReader()", "binary ${frame.data.size}B")
                    is Frame.Close -> log("launchReader()", "close frame: ${frame.readReason()?.message}")
                    is Frame.Pong -> log("launchReader()", "pong")
                    else -> log("launchReader()", "")
                }
            }
        }.onFailure { e ->
            log("launchReader()", "reader: ${e.message}")
            _events.emit(ServerEvent.Closed(e.message))
        }

        cleanup()
    }

    private fun launchPinger() = scope.launch {
        while (isActive) {
            delay(5.seconds)
            log("PING", "sending ping")
            send(WsMessage(type = "ping", clientTimestamp = System.currentTimeMillis()))
        }
    }

    private fun cleanup() {
        log("CLEANUP", "tearing down session")
        session = null
        _state.value = ConnectionState.Disconnected
        if (reconnectJob?.isActive != true) {
            reconnectJob = scope.launch { connectWithRetry() }
        }
    }

    private suspend fun connectWithRetry(currentBackoff: Long = 500L) {
        _state.value = ConnectionState.Connecting

        runCatching {
            client.webSocketSession(baseurl).also { ws ->
                session = ws
                _state.value = ConnectionState.Connected
                launchReader(ws)
                launchPinger()
            }
        }.onFailure {
            val jitter = currentBackoff / 4
            val wait = currentBackoff + Random.nextLong(-jitter, jitter)
            log("RETRY", "retrying in ${wait}ms")
            delay(wait)
            connectWithRetry(min(currentBackoff * 2, 3_600_000L))
        }
    }

    // -------- Repo selection
    fun selectRepoFlow(
        repo: GitHubRepoData,
        accessToken: String?
    ): Flow<GitHubRepoSelectResponse> = channelFlow {
        val id = UUID.randomUUID().toString()
        log("FLOW", "selectRepo start id=$id repo=${repo.owner}/${repo.name} token=${accessToken}")

        val collector = launch {
            events.collect { ev ->
                val r = (ev as? ServerEvent.Repo)?.value ?: return@collect
                if (r.messageId == id) {
                    log("FLOW", "selectRepo progress id=$id status=${r.status} progress=${r.progress} msg=${r.message}")
                    trySend(r)
                    if (r.status == "success" || r.status == "error") cancel()
                }
            }
        }

        send(GitHubRepoSelectMessage(
            type = "workspace_select_github",
            messageId = id,
            repoData = repo,
            accessToken = accessToken,
            clientTimestamp = System.currentTimeMillis()
        ))
        awaitClose { collector.cancel() }
    }

    fun startContainerFlow(openaiApiKey: String? = null): Flow<ContainerResponse> = channelFlow {
        val id = UUID.randomUUID().toString()
        val msg = ContainerMessage(type = "arcana_start", messageId = id, openaiApiKey = openaiApiKey)
        log("FLOW", "container start id=$id key=${openaiApiKey}")

        val collector = launch {
            events.collect { ev ->
                val c = (ev as? ServerEvent.Container)?.value ?: return@collect
                if (c.messageId == id) trySend(c)
            }
        }

        send(msg)
        awaitClose { collector.cancel() }
    }

    suspend fun sendContainerInput(text: String) {
        val preview = text.replace("\n", "\\n").let { it.substring(0, min(200, it.length)) }
        log("SEND", "container_input ${text.length}B: $preview${if (text.length > 200) "…" else ""}")
        send(ContainerMessage(type = "container_input", input = text))
    }

    suspend fun stopContainer() {
        log("SEND", "container_stop")
        send(ContainerMessage(type = "container_stop"))
    }

    // -------- internals
    private suspend fun parseReceived(text: String) {
        val el = json.parseToJsonElement(text)
        val type = el.jsonObject["type"]?.jsonPrimitive?.content?.lowercase()
        val event = when (type) {
            "workspace_select_github_response" -> runCatching {
                json.decodeFromJsonElement<GitHubRepoSelectResponse>(el)
            }.onSuccess {
                log("PARSE", "repoResponse id=${it.messageId} status=${it.status} progress=${it.progress} msg=${it.message}")
            }.map(ServerEvent::Repo).getOrElse { ServerEvent.Raw(text) }

            "container_response" -> runCatching {
                json.decodeFromJsonElement<ContainerResponse>(el)
            }.onSuccess {
                log("PARSE", "containerResponse id=${it.messageId} status=${it.status} outputLen=${it.output?.length ?: 0}")
            }.map(ServerEvent::Container).getOrElse { ServerEvent.Raw(text) }

            "pong" -> runCatching {
                json.decodeFromJsonElement<WsMessage>(el)
            }.onSuccess {
                log("PARSE", "pong ts=${it.serverTimestamp}")
                handlePong(it)
            }.map(ServerEvent::Pong).getOrElse { ServerEvent.Raw(text) }

            else -> {
                // Fallback: try decoding known payloads even when "type" is missing
                runCatching { json.decodeFromJsonElement<GitHubRepoSelectResponse>(el) }
                    .onSuccess {
                        log("PARSE", "repoResponse(no-type) id=${it.messageId} status=${it.status} progress=${it.progress} msg=${it.message}")
                    }
                    .map(ServerEvent::Repo)
                    .recoverCatching {
                        json.decodeFromJsonElement<ContainerResponse>(el).also { c ->
                            log("PARSE", "containerResponse(no-type) id=${c.messageId} status=${c.status} outputLen=${c.output?.length ?: 0}")
                        }.let(ServerEvent::Container)
                    }
                    .getOrElse {
                        log("PARSE", "raw unknown: $text")
                        ServerEvent.Raw(text)
                    }
            }
        }
        _events.emit(event)
    }

    private fun handlePong(msg: WsMessage) {
        val latencyMs = System.currentTimeMillis() - (msg.clientTimestamp ?: return)
        _latency.value = latencyMs
        log("RTT", "$latencyMs ms")
    }

    private suspend inline fun <reified T> send(payload: T) {
        val js = json.encodeToString(payload)
        log("SEND", "${payload!!::class.simpleName ?: "payload"} ${js.length}B: ${js.substring(0..200)}}")
        val s = session ?: run {
            log("ERROR", "send: no session")
            error("WS not connected")
        }
        s.send(Frame.Text(js))
    }
}

@Composable
fun HeartbeatIndicator(modifier: Modifier = Modifier) {
    val client = LocalDI.current.websocketClient
    val state by client.state.collectAsState()
    val latency by client.latency.collectAsState()
    val connected = state == ConnectionState.Connected

    // --- Realistic heartbeat (double pulse, differing peaks)
    val beat = rememberInfiniteTransition(label = "hb")
    val pulse by beat.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2200
                0f at 0 using LinearOutSlowInEasing
                1f at 1100 using FastOutSlowInEasing
                0f at 2200 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Neon color per state
    val neon = when (state) {
        ConnectionState.Connected -> Color(0xFF00FF9D) // cyan-green neon
        ConnectionState.Connecting -> Color(0xFFFFC244) // warm amber
        ConnectionState.Disconnected -> Color(0xFFFF4B4B) // red
    }

    // Halo parameters (room outside the dot so nothing clips)
    val haloMin = 2.dp // 10.dp
    val haloMax = 24.dp // 40.dp
    val haloRadius = if (connected) lerp(haloMin, haloMax, 0.6f + (0.4f * pulse)) else 0.dp
    val haloAlpha = if (connected) lerp(0.5f, 0.8f, pulse) else 0f
    val dotSize = 12.dp
    val glowPad = 24.dp // padding to give halo room outside bounds

    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Glow halo outside
            Box(
                modifier = Modifier.size(dotSize + glowPad * 2),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            if (haloAlpha > 0f) {
                                val radiusPx = haloRadius.toPx().coerceAtLeast(1f)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        0f to neon.copy(alpha = haloAlpha * 0.6f),
                                        0.5f to neon.copy(alpha = haloAlpha * 0.3f),
                                        1f to Color.Transparent,
                                        radius = radiusPx
                                    ),
                                    radius = radiusPx,
                                    center = center
                                )
                            }
                        }.blur(radius = 16.dp) // blur da Halo
                )

                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .background(neon, CircleShape)
                        .innerShadow(CircleShape, Shadow(radius = 2.6.dp, color = Color.Black.copy(alpha = 0.8f), spread = 0.8.dp, offset = DpOffset(1.5.dp, 1.dp)))
                )
            }

            Text(
                text = when (state) {
                    ConnectionState.Connected -> "Online"
                    ConnectionState.Connecting -> "Connecting..."
                    ConnectionState.Disconnected -> "Offline"
                },
                style = MaterialTheme.typography.body2,
                color = Color.White
            )
        }

        latency?.let { ms ->
            val latencyColor = when {
                ms < 100 -> Color(0xFF34D399)
                ms < 250 -> Color(0xFFFBBF24)
                ms < 500 -> Color(0xFFF97316)
                else -> Color(0xFFEF4444)
            }
            Text("Latency: ${ms}ms", style = MaterialTheme.typography.caption, color = latencyColor)
        }
    }
}
