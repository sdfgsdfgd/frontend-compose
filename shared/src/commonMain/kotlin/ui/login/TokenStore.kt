package ui.login

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import ui.login.model.AccessToken
import ui.login.model.GithubEmail
import ui.login.model.GithubUser

object TokenStore {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** File lives in the current working directory alongside the exe/JAR  */
    private fun prefsPath(): Path = AppDirs.path / "github_prefs.preferences_pb"

    private val store = PreferenceDataStoreFactory.create(
        storage = OkioStorage(
            serializer  = PreferencesSerializer,
            fileSystem  = FileSystem.SYSTEM,
            producePath = ::prefsPath
        ),
        scope = scope
    )

    /* ── keys ─────────────────────────────────────────────────────── */
    private val TOKEN  = stringPreferencesKey("gh.token")
    private val SCOPES = stringPreferencesKey("gh.scopes")
    private val USER   = stringPreferencesKey("gh.user")
    private val EMAILS = stringPreferencesKey("gh.emails")

    /* ── save ─────────────────────────────────────────────────────── */
    suspend fun save(token: AccessToken, user: GithubUser, emails: List<GithubEmail>) {
        store.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                this[TOKEN]  = token.accessToken
                this[SCOPES] = token.scope
                this[USER]   = Json.encodeToString(user)
                this[EMAILS] = Json.encodeToString(emails)
            }
        }
    }

    /* ── load ─────────────────────────────────────────────────────── */
    suspend fun load(): Triple<String, GithubUser, List<GithubEmail>>? =
        store.data.map { p ->
            val t = p[TOKEN] ?: return@map null

            val u = p[USER]
                ?.let { Json.decodeFromString<GithubUser>(it) }
                ?: return@map null

            val e = p[EMAILS]
                ?.let { Json.decodeFromString<List<GithubEmail>>(it) }
                ?: emptyList()

            Triple(t, u, e)
        }.firstOrNull()

    /* ── clear ────────────────────────────────────────────────────── */
    fun clear() = scope.launch { store.updateData { emptyPreferences() } }
}