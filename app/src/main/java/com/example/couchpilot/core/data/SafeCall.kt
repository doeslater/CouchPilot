package com.example.couchpilot.core.data

import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import java.io.IOException

/**
 * Typed GET — builds the request, runs it through [safeCall], and deserializes the body.
 * [block] lets a call site add anything request-specific (e.g. an auth header) without every
 * data source needing to know about Ktor's [HttpRequestBuilder] directly.
 */
suspend inline fun <reified T> HttpClient.get(
    url: String,
    queryParameters: Map<String, Any?> = mapOf(),
    block: HttpRequestBuilder.() -> Unit = {},
): Result<T, DataError.Network> {
    return safeCall {
        get {
            url(url)
            queryParameters.forEach { (key, value) -> if (value != null) parameter(key, value) }
            block()
        }
    }
}

/** Runs [execute], converting the exceptions a Ktor call can throw into a typed [DataError.Network]. */
suspend inline fun <reified T> safeCall(execute: () -> HttpResponse): Result<T, DataError.Network> {
    val response = try {
        execute()
    } catch (e: IOException) {
        // Covers UnknownHostException / ConnectException / SocketTimeoutException, etc.
        return Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: SerializationException) {
        return Result.Error(DataError.Network.SERIALIZATION)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        return Result.Error(DataError.Network.UNKNOWN)
    }
    return responseToResult(response)
}

suspend inline fun <reified T> responseToResult(response: HttpResponse): Result<T, DataError.Network> {
    return when (response.status.value) {
        in 200..299 -> try {
            Result.Success(response.body())
        } catch (e: SerializationException) {
            Result.Error(DataError.Network.SERIALIZATION)
        }
        400 -> Result.Error(DataError.Network.BAD_REQUEST)
        401 -> Result.Error(DataError.Network.UNAUTHORIZED)
        403 -> Result.Error(DataError.Network.FORBIDDEN)
        404 -> Result.Error(DataError.Network.NOT_FOUND)
        408 -> Result.Error(DataError.Network.REQUEST_TIMEOUT)
        409 -> Result.Error(DataError.Network.CONFLICT)
        413 -> Result.Error(DataError.Network.PAYLOAD_TOO_LARGE)
        429 -> Result.Error(DataError.Network.TOO_MANY_REQUESTS)
        in 500..599 -> Result.Error(DataError.Network.SERVER_ERROR)
        else -> Result.Error(DataError.Network.UNKNOWN)
    }
}
