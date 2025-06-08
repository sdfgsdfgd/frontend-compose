package domain

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import data.model.AccessToken
import data.model.GithubEmail
import data.model.GithubUser
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
import platform.AppDirs

object DataStore {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** xx    `AppDirs`  -->  platform specific paths !
     *
     *  Slack discussion on it  ( for ultimate solutions ):
     *   https://kotlinlang.slack.com/archives/C01D6HTPATV/p1747178681489469
     * */
    private fun prefsPath(): Path = AppDirs.path / ".github_prefs.preferences_pb"

    private val store = PreferenceDataStoreFactory.create(
        storage = OkioStorage(
            serializer  = PreferencesSerializer,
            fileSystem  = FileSystem.SYSTEM,
            producePath = DataStore::prefsPath
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
    suspend fun load(): CachedSession? = store.data.map { p ->
        val tok = p[TOKEN]  ?: return@map null
        val scp = p[SCOPES] ?: ""
        val usr = p[USER ]?.let { Json.decodeFromString<GithubUser>(it) } ?: return@map null
        val eml = p[EMAILS]?.let { Json.decodeFromString<List<GithubEmail>>(it) } ?: emptyList()

        CachedSession(tok, scp, usr, eml)
    }.firstOrNull()

    /* ── clear ────────────────────────────────────────────────────── */
    fun clear() = scope.launch { store.updateData { emptyPreferences() } }
}

data class CachedSession(
    val token:  String,              // raw access-token
    val scope:  String,              // space-separated scopes
    val user: GithubUser,
    val emails: List<GithubEmail>
)