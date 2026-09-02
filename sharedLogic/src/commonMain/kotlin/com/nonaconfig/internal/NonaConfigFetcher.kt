package com.nonaconfig.internal

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

import io.ktor.http.*

internal class NonaConfigFetcher(
    private val apiKey: String,
    private val environmentId: String,
    private val baseUrl: String = "https://nona-config.ryware.io",
    httpClient: HttpClient? = null
) {
    private val client = httpClient ?: HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    sealed class FetchResult {
        data class Success(val config: Map<String, String>, val eTag: String?) : FetchResult()
        object NotModified : FetchResult()
        data class Error(val exception: Exception) : FetchResult()
    }

    suspend fun fetchAll(eTag: String?, version: String? = null): FetchResult {
        return try {
            val response = client.get("$baseUrl/api/$environmentId") {
                header("X-Api-Key", apiKey)
                if (eTag != null) {
                    header(HttpHeaders.IfNoneMatch, eTag)
                }
                if (version != null) {
                    parameter("version", version)
                }
            }

            if (response.status == HttpStatusCode.NotModified) {
                FetchResult.NotModified
            } else if (response.status == HttpStatusCode.OK) {
                val config: Map<String, String> = response.body()
                val newETag = response.headers[HttpHeaders.ETag]
                FetchResult.Success(config, newETag)
            } else {
                FetchResult.Error(Exception("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            FetchResult.Error(e)
        }
    }
}
