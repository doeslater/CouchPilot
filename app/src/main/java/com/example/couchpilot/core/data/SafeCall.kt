package com.example.couchpilot.core.data

import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException

/**
 * Runs [execute], converting Retrofit [Response] and potential exceptions into a typed [Result].
 */
suspend inline fun <reified T> safeCall(execute: () -> Response<T>): Result<T, DataError.Network> {
    val response = try {
        execute()
    } catch (e: IOException) {
        return Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: JsonParseException) {
        return Result.Error(DataError.Network.SERIALIZATION)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        return Result.Error(DataError.Network.UNKNOWN)
    }
    return responseToResult(response)
}

fun <T> responseToResult(response: Response<T>): Result<T, DataError.Network> {
    return if (response.isSuccessful) {
        val body = response.body()
        if (body != null) {
            Result.Success(body)
        } else {
            Result.Error(DataError.Network.SERIALIZATION)
        }
    } else {
        when (response.code()) {
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
}
