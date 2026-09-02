package com.nonaconfig

import com.nonaconfig.internal.NonaConfigFetcher
import com.russhwolf.settings.MapSettings
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class NonaConfigTest {

    private fun setupMockedConfig(engine: MockEngine): NonaConfig {
        val nonaConfig = NonaConfig.instance
        val settings = MapSettings()
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        nonaConfig.initializeForTest("key", "env", settings, client)
        return nonaConfig
    }

    @Test
    fun testDefaultsAndRetrieval() {
        val nonaConfig = NonaConfig.instance
        nonaConfig.initialize("key", "env") // Standard init for simple test
        
        nonaConfig.setDefaults(mapOf("key1" to "default"))
        assertEquals("default", nonaConfig.getString("key1"))
        assertEquals("", nonaConfig.getString("unknown"))
    }

    @Test
    fun testFetchAndActivateSuccess() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = "{\"key1\": \"remote\"}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val nonaConfig = setupMockedConfig(engine)
        nonaConfig.setDefaults(mapOf("key1" to "default"))

        // Before fetch
        assertEquals("default", nonaConfig.getString("key1"))

        // Fetch
        val fetchResult = nonaConfig.fetch()
        assertTrue(fetchResult)

        // After fetch, before activate
        assertEquals("default", nonaConfig.getString("key1"))

        // Activate
        val activateResult = nonaConfig.activate()
        assertTrue(activateResult)

        // After activate
        assertEquals("remote", nonaConfig.getString("key1"))
    }

    @Test
    fun testFetchThrottling() = runTest {
        val engine = MockEngine { _ ->
            respond(content = "{}", status = HttpStatusCode.OK)
        }
        val nonaConfig = setupMockedConfig(engine)
        nonaConfig.setConfigSettings(
            NonaConfigSettings.Builder()
                .setMinimumFetchInterval(10.seconds)
                .build()
        )

        // First fetch
        assertTrue(nonaConfig.fetch())

        // Second fetch immediately should be throttled
        assertFalse(nonaConfig.fetch())
    }

    @Test
    fun testFetchAndActivateCombined() = runTest {
        val engine = MockEngine { _ ->
            respond(content = "{\"k\": \"v\"}", status = HttpStatusCode.OK)
        }
        val nonaConfig = setupMockedConfig(engine)
        
        assertTrue(nonaConfig.fetchAndActivate())
        assertEquals("v", nonaConfig.getString("k"))
    }

    @Test
    fun testFetchFailure() = runTest {
        val engine = MockEngine { _ ->
            respond(content = "Error", status = HttpStatusCode.InternalServerError)
        }
        val nonaConfig = setupMockedConfig(engine)
        
        assertFalse(nonaConfig.fetch())
        assertFalse(nonaConfig.fetchAndActivate())
    }

    @Test
    fun testActivateWithNoFetchedData() {
        val nonaConfig = NonaConfig.instance
        nonaConfig.initialize("key", "env")
        // No fetch called
        assertFalse(nonaConfig.activate())
    }
}
