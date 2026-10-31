package me.aysh.pocketledger.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.sessionDataStore by preferencesDataStore(name = "session")

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val token: String) : SessionState
}

/** Persists the bearer token, encrypted with [TokenCipher], and exposes it as observable state. */
class SessionStore(context: Context, private val cipher: TokenCipher, scope: CoroutineScope) {

    private val dataStore = context.applicationContext.sessionDataStore

    val state: StateFlow<SessionState> = dataStore.data
        .map { prefs ->
            prefs[TOKEN_KEY]?.let(cipher::decrypt)?.let { SessionState.SignedIn(it) } ?: SessionState.SignedOut
        }
        .stateIn(scope, SharingStarted.Eagerly, SessionState.Loading)

    val currentToken: String? get() = (state.value as? SessionState.SignedIn)?.token

    suspend fun save(token: String) {
        val encrypted = cipher.encrypt(token)
        dataStore.edit { it[TOKEN_KEY] = encrypted }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(TOKEN_KEY) }
    }

    private companion object {
        val TOKEN_KEY = stringPreferencesKey("token")
    }
}
