package com.nonaconfig.internal

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.*

class NonaConfigFetcherTest {

    private fun createFetcher(engine: MockEngine): NonaConfigFetcher {
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return NonaConfigFetcher("key", "env", httpClient = client)
    }

    @Test
    fun testFetchSuccess() = runTest {
        val engine = MockEngine { request ->
            assertEquals("key", request.headers["X-Api-Key"])
            respond(
                content = "{\"key\": \"value\"}",
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf("application/json"),
                    HttpHeaders.ETag to listOf("etag123")
                )
            )
        }
        val fetcher = createFetcher(engine)
        val result = fetcher.fetchAll(null)

        assertTrue(result is NonaConfigFetcher.FetchResult.Success)
        assertEquals(mapOf("key" to "value"), result.config)
        assertEquals("etag123", result.eTag)
    }

    @Test
    fun testFetchNotModified() = runTest {
        val engine = MockEngine { request ->
            assertEquals("etag123", request.headers[HttpHeaders.IfNoneMatch])
            respond(content = "", status = HttpStatusCode.NotModified)
        }
        val fetcher = createFetcher(engine)
        val result = fetcher.fetchAll("etag123")

        assertTrue(result is NonaConfigFetcher.FetchResult.NotModified)
    }

    @Test
    fun testFetchError() = runTest {
        val engine = MockEngine { _ ->
            respond(content = "Error", status = HttpStatusCode.InternalServerError)
        }
        val fetcher = createFetcher(engine)
        val result = fetcher.fetchAll(null)

        assertTrue(result is NonaConfigFetcher.FetchResult.Error)
    }

    @Test
    fun testFetchException() = runTest {
        val engine = MockEngine { _ ->
            throw Exception("Network Fail")
        }
        val fetcher = createFetcher(engine)
        val result = fetcher.fetchAll(null)

        assertTrue(result is NonaConfigFetcher.FetchResult.Error)
        assertEquals("Network Fail", result.exception.message)
    }

    @Test
    fun testFetchWithVersion() = runTest {
        val engine = MockEngine { request ->
            assertTrue(request.url.parameters.contains("version", "1.0.0"))
            respond(content = "{}", status = HttpStatusCode.OK)
        }
        val fetcher = createFetcher(engine)
        fetcher.fetchAll(null, version = "1.0.0")
    }
}
