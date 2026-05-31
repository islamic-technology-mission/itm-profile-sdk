package com.itm.profile_sdk.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

internal class InternalTokenService {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun generateToken(uid: String, internalKey: String): GenerateTokenResponse {
        return client.post("$BASE_URL/api/v1/internal/generate-token") {
            header("X-Internal-Key", internalKey)
            contentType(ContentType.Application.Json)
            setBody(GenerateTokenRequest(uid = uid))
        }.body()
    }

    companion object {
        private const val BASE_URL = "https://sandbox.theislam360.com"
    }
}

