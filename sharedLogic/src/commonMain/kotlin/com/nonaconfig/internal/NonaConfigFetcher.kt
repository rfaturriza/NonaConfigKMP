package com.nonaconfig.internal

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal class NonaConfigFetcher(
    private val apiKey: String,
    private val environmentId: String,
    private val baseUrl: String,
    httpClient: HttpClient? = null
) {
    private val jsonInstance = Json { ignoreUnknownKeys = true }

    private val client = httpClient ?: HttpClient {
        install(ContentNegotiation) {
            json(jsonInstance)
        }
    }

    sealed class FetchResult {
        data class Success(val config: Map<String, String>, val eTag: String?) : FetchResult()
        object NotModified : FetchResult()
        data class Error(val exception: Exception) : FetchResult()
    }

    suspend fun fetchAll(
        eTag: String?,
        version: String? = null,
        prefix: String? = null
    ): FetchResult {
        return try {
            val endpointPath = when {
                version.isNullOrBlank() ->
                    "$baseUrl/api/environments/$environmentId/parameters"

                version.equals("active", ignoreCase = true) ->
                    "$baseUrl/api/environments/$environmentId/releases/active/parameters"

                else ->
                    "$baseUrl/api/environments/$environmentId/releases/$version/parameters"
            }

            val response = client.get(endpointPath) {
                header("X-Api-Key", apiKey)
                if (eTag != null) {
                    header(HttpHeaders.IfNoneMatch, eTag)
                }
                if (!prefix.isNullOrBlank()) {
                    parameter("prefix", prefix)
                }
            }

            when (response.status) {
                HttpStatusCode.NotModified -> FetchResult.NotModified
                HttpStatusCode.OK -> {
                    val jsonText = response.bodyAsText()
                    val jsonElement = jsonInstance.parseToJsonElement(jsonText)
                    val configMap = mutableMapOf<String, String>()

                    if (jsonElement is JsonObject) {
                        for ((key, element) in jsonElement) {
                            val valueString = when (element) {
                                is JsonObject -> {
                                    when (val valueChild = element["value"]) {
                                        is JsonPrimitive -> valueChild.content
                                        null -> element.toString()
                                        else -> valueChild.toString()
                                    }
                                }

                                is JsonPrimitive -> element.content
                                else -> element.toString()
                            }
                            configMap[key] = valueString
                        }
                    }

                    val newETag = response.headers[HttpHeaders.ETag]
                    FetchResult.Success(configMap, newETag)
                }

                HttpStatusCode.Unauthorized ->
                    FetchResult.Error(Exception("401 Unauthorized: Invalid or missing X-Api-Key, or scope cannot read entry."))

                HttpStatusCode.NotFound ->
                    FetchResult.Error(Exception("404 Not Found: Environment '$environmentId', release '$version', or route not found."))

                HttpStatusCode.Conflict ->
                    FetchResult.Error(Exception("409 Conflict: Active release requested but no active release is configured."))

                else ->
                    FetchResult.Error(Exception("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            FetchResult.Error(e)
        }
    }
}
