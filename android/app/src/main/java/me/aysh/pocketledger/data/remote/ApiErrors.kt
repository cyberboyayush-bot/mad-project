package me.aysh.pocketledger.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** A failure with a message that is safe and friendly to show to the user. */
class AppException(override val message: String, val status: Int? = null, cause: Throwable? = null) :
    Exception(message, cause)

internal object ApiErrors {

    fun fromStatus(status: Int, rawBody: String?, json: Json): AppException {
        val body = rawBody?.let { runCatching { json.decodeFromString<ErrorBody>(it) }.getOrNull() }
        val code = body?.error?.code ?: body?.code
        val serverMessage = body?.error?.message ?: body?.message

        val message = when {
            status == 401 && code == "INVALID_EMAIL_OR_PASSWORD" -> "Incorrect email or password"
            status == 401 -> "Your session has expired. Please sign in again."
            status == 429 -> "Too many attempts. Please wait a minute and try again."
            code?.contains("ALREADY_EXISTS") == true -> "An account with this email already exists"
            code == "INVALID_EMAIL" -> "Please enter a valid email address"
            code == "PASSWORD_TOO_SHORT" -> "Password must be at least 8 characters"
            code == "PASSWORD_TOO_LONG" -> "Password is too long"
            status == 404 -> "That item no longer exists"
            status >= 500 -> "The server ran into a problem. Please try again."
            !serverMessage.isNullOrBlank() -> serverMessage
            else -> "Something went wrong (error $status)"
        }
        return AppException(message, status)
    }

    fun from(throwable: Throwable, json: Json): AppException = when (throwable) {
        is AppException -> throwable
        is HttpException -> fromStatus(throwable.code(), throwable.response()?.errorBody()?.string(), json)
        is IOException -> AppException("Can't reach the server. Check that it's running and try again.", cause = throwable)
        is SerializationException -> AppException("Received an unexpected response from the server.", cause = throwable)
        else -> AppException("Something went wrong. Please try again.", cause = throwable)
    }
}

/** Runs a network call and converts every failure into an [AppException]. */
internal suspend fun <T> apiCall(json: Json, block: suspend () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        throw ApiErrors.from(e, json)
    }

/** Unwraps a raw [Response], throwing an [AppException] for non-2xx responses. */
internal fun <T> Response<T>.requireSuccess(json: Json): Response<T> {
    if (isSuccessful) return this
    throw ApiErrors.fromStatus(code(), errorBody()?.string(), json)
}
