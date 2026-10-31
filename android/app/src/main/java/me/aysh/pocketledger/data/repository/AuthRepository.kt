package me.aysh.pocketledger.data.repository

import kotlinx.serialization.json.Json
import me.aysh.pocketledger.data.remote.AppException
import me.aysh.pocketledger.data.remote.AuthResponse
import me.aysh.pocketledger.data.remote.PocketLedgerApi
import me.aysh.pocketledger.data.remote.SignInRequest
import me.aysh.pocketledger.data.remote.SignUpRequest
import me.aysh.pocketledger.data.remote.apiCall
import me.aysh.pocketledger.data.remote.requireSuccess
import retrofit2.Response

/** Abstraction over where the session token is kept, so the repository is testable on the JVM. */
interface TokenStore {
    suspend fun save(token: String)
    suspend fun clear()
}

class AuthRepository(
    private val api: PocketLedgerApi,
    private val tokens: TokenStore,
    private val json: Json,
) {

    suspend fun signUp(name: String, email: String, password: String) {
        val token = apiCall(json) {
            api.signUp(SignUpRequest(name.trim(), email.trim().lowercase(), password)).requireToken()
        }
        tokens.save(token)
    }

    suspend fun signIn(email: String, password: String) {
        val token = apiCall(json) {
            api.signIn(SignInRequest(email.trim().lowercase(), password)).requireToken()
        }
        tokens.save(token)
    }

    /** Revokes the session on the server when possible, and always forgets it locally. */
    suspend fun signOut() {
        try {
            apiCall(json) { api.signOut() }
        } catch (_: AppException) {
            // Offline or already expired: clearing locally is still the right outcome.
        } finally {
            tokens.clear()
        }
    }

    private fun Response<AuthResponse>.requireToken(): String {
        requireSuccess(json)
        return headers()[AUTH_TOKEN_HEADER]?.takeIf { it.isNotBlank() }
            ?: throw AppException("The server didn't return a session. Please try again.")
    }

    private companion object {
        const val AUTH_TOKEN_HEADER = "set-auth-token"
    }
}
