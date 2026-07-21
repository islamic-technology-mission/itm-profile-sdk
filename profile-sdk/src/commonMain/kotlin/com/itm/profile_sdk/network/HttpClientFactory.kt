package com.itm.profile_sdk.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

internal object HttpClientFactory {

    fun create(baseUrl: String): HttpClient {
        return HttpClient {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient         = true
                    prettyPrint       = false
                })
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level  = LogLevel.BODY
            }
            install(DefaultRequest) {
                url(baseUrl)
            }
            // Parse API error bodies so callers get the real message instead of null.
            install(HttpCallValidator) {
                handleResponseExceptionWithRequest { exception, _ ->
                    val response = (exception as? ResponseException)?.response ?: return@handleResponseExceptionWithRequest
                    val body = response.bodyAsText()
                    val apiError = runCatching { errorJson.decodeFromString<ApiErrorResponse>(body) }.getOrNull()
                    val message = apiError?.message ?: "HTTP ${response.status.value}"
                    val code = response.status.value
                    val detailedMessage = if (code == 400 && !apiError?.errors.isNullOrEmpty()) {
                        val fieldErrors = apiError.errors.entries.joinToString("; ") { (field, messages) ->
                            "$field: ${messages.joinToString(", ")}"
                        }
                        "$message: $fieldErrors"
                    } else message
                    throw when (code) {
                        400  -> ApiException.BadRequest(detailedMessage, apiError?.errors)
                        401  -> ApiException.Unauthorized(message)
                        403  -> ApiException.Forbidden(message)
                        404  -> ApiException.NotFound(message)
                        in 500..599 -> ApiException.ServerError(message, code)
                        else -> ApiException.Unknown(message, code)
                    }
                }
            }
        }
    }
}
