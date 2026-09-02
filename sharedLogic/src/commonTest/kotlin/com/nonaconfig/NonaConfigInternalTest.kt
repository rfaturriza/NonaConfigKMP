package com.nonaconfig.internal

import com.russhwolf.settings.MapSettings
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.*

class NonaConfigInternalTest {

    @Test
    fun testStorage() {
        val settings = MapSettings()
        val storage = NonaConfigStorage(settings)

        val config = mapOf("key1" to "value1", "key2" to "true")
        storage.saveFetchedConfig(config)
        assertEquals(config, storage.getFetchedConfig())

        storage.saveActiveConfig(config)
        assertEquals(config, storage.getActiveConfig())

        val defaults = mapOf("def" to "val")
        storage.saveDefaults(defaults)
        assertEquals(defaults, storage.getDefaults())

        storage.lastFetchTime = 12345L
        assertEquals(12345L, storage.lastFetchTime)

        storage.eTag = "abc"
        assertEquals("abc", storage.eTag)
        storage.eTag = null
        assertNull(storage.eTag)
    }

    @Test
    fun testFetcherSuccess() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = "{\"key\": \"value\"}",
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf("application/json"),
                    HttpHeaders.ETag to listOf("new-etag")
                )
            )
        }
        
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val fetcher = NonaConfigFetcher("api-key", "env", httpClient = client)
        val result = fetcher.fetchAll(null)
        
        assertTrue(result is NonaConfigFetcher.FetchResult.Success)
        assertEquals("value", result.config["key"])
        assertEquals("new-etag", result.eTag)
    }
}
