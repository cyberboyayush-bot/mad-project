package me.aysh.pocketledger

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.aysh.pocketledger.data.remote.AppJson
import me.aysh.pocketledger.data.remote.createApi
import me.aysh.pocketledger.data.repository.AuthRepository
import me.aysh.pocketledger.data.repository.LedgerRepository
import me.aysh.pocketledger.data.repository.TokenStore
import me.aysh.pocketledger.data.session.SessionStore
import me.aysh.pocketledger.data.session.TokenCipher

/** Manual dependency container: one instance of each shared object for the whole app. */
class AppContainer(context: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val sessionStore = SessionStore(context, TokenCipher(), appScope)

    private val api = createApi(
        baseUrl = BuildConfig.API_BASE_URL,
        tokenProvider = { sessionStore.currentToken },
        onUnauthorized = { appScope.launch { sessionStore.clear() } },
    )

    val authRepository = AuthRepository(
        api = api,
        tokens = object : TokenStore {
            override suspend fun save(token: String) = sessionStore.save(token)
            override suspend fun clear() = sessionStore.clear()
        },
        json = AppJson,
    )

    val ledgerRepository = LedgerRepository(api, AppJson)
}
