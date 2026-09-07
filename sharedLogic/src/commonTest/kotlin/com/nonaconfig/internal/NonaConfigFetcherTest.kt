package com.nonaconfig.internal

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NonaConfigFetcherTest {

    private fun createFetcher(engine: MockEngine): NonaConfigFetcher {
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return NonaConfigFetcher("key", "env", httpClient = client, baseUrl = "localhost")
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

    @Test
    fun testFetchResultDataClassMethods() {
        val s1 = NonaConfigFetcher.FetchResult.Success(mapOf("a" to "b"), "tag")
        val (config, tag) = s1
        assertEquals(mapOf("a" to "b"), config)
        assertEquals("tag", tag)
        assertEquals(s1, s1.copy())
        assertEquals(s1.hashCode(), s1.copy().hashCode())
        assertTrue(s1.toString().contains("Success"))

        val ex = Exception("fail")
        val e1 = NonaConfigFetcher.FetchResult.Error(ex)
        assertEquals(ex, e1.exception)
        assertEquals(e1, e1.copy())
        assertEquals(e1.hashCode(), e1.copy().hashCode())
        assertTrue(e1.toString().contains("Error"))

        val notMod = NonaConfigFetcher.FetchResult.NotModified
        assertNotNull(notMod.toString())
    }
}
