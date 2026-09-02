package com.nonaconfig.internal

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

internal class NonaConfigFetcher(
    private val apiKey: String,
    private val environmentId: String,
    private val baseUrl: String = "https://nona-config.ryware.io" // Replace with actual base URL if different
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun fetchAll(): Map<String, String> {
        return try {
            // Based on the documentation, GET /api/{environmentId} fetches all client-visible keys.
            // Since we don't have the exact JSON format for 'all keys', 
            // we assume it returns a Map<String, String> for now.
            val response: Map<String, String> = client.get("$baseUrl/api/$environmentId") {
                header("X-Api-Key", apiKey)
            }.body()
            response
        } catch (e: Exception) {
            // Fallback: If bulk fetch fails or is not implemented as expected, 
            // we might need a different strategy.
            throw e
        }
    }
}
