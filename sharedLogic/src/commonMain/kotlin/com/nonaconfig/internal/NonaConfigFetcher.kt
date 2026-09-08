package com.nonaconfig.internal

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

import io.ktor.http.*

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
            } else {
                FetchResult.Error(Exception("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            FetchResult.Error(e)
        }
    }
}
