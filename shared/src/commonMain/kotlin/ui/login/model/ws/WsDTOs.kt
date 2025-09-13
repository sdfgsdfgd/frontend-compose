package ui.login.model.ws

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

// TODO-1: !!!Shared DTOs across stack:  Create module/ shared between client/server, unify client + server DTOs !!!
sealed interface SyncStatus {
    data object Initializing : SyncStatus
    data object Syncing : SyncStatus
    data object Synchronized : SyncStatus
    data class Error(val reason: String? = null) : SyncStatus
}

@Immutable
data class SyncUiState(
    val status: SyncStatus = SyncStatus.Initializing,
    val progress: Int = 0,                 // 0..100
    val message: String? = null
)


@Serializable
data class GitHubRepoData(
    val repoId: Long? = null,
    val name: String = "",
    val owner: String = "",
    val url: String = "",
    val branch: String? = null
)

@Serializable
data class GitHubRepoSelectMessage(
    val type: String, // "workspace_select_github"
    val messageId: String,
    val repoData: GitHubRepoData,
    val accessToken: String? = null,
    val clientTimestamp: Long
)

@Serializable
data class GitHubRepoSelectResponse(
    val type: String = "workspace_select_github_response",
    val messageId: String,
    val status: String,          // "success" | "error" | "cloning"
    val message: String,
    val workspaceId: String? = null,
    val progress: Int? = null,
    val serverTimestamp: Long
)

@Serializable
data class ContainerMessage(
    val type: String, // "arcana_start" | "container_input" | "container_stop"
    val messageId: String? = null,
    val script: String? = null,
    val input: String? = null,
    val clientTimestamp: Long? = System.currentTimeMillis(),
    val openaiApiKey: String? = null
)

@Serializable
data class ContainerResponse(
    val type: String = "container_response",
    val messageId: String,
    val status: String, // "starting" | "running" | "input_needed" | "error" | "exited"
    val output: String? = null,
    val serverTimestamp: Long = System.currentTimeMillis()
)

@Serializable
data class WsMessage(
    val type: String,
    val clientTimestamp: Long? = null,
    val serverTimestamp: Long? = null,
    val payload: String? = null
)

sealed interface ServerEvent {
    data class Repo(val value: GitHubRepoSelectResponse) : ServerEvent
    data class Container(val value: ContainerResponse) : ServerEvent
    data class Pong(val value: WsMessage) : ServerEvent
    data class Raw(val json: String) : ServerEvent
    data class Closed(val reason: String?) : ServerEvent
}