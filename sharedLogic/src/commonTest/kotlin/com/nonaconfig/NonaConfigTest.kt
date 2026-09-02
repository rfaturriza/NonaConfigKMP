package com.nonaconfig

import com.russhwolf.settings.MapSettings
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

@Serializable
data class TestJson(val key: String, val value: Int)

class NonaConfigTest {

    private fun createTestConfig(engine: MockEngine): NonaConfig {
        val nonaConfig = NonaConfig()
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
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        val nonaConfig = createTestConfig(engine)
        
        nonaConfig.setDefaults(mapOf("key1" to "default", "key2" to true))
        assertEquals("default", nonaConfig.getString("key1"))
        assertEquals(true, nonaConfig.getBoolean("key2"))
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
        val nonaConfig = createTestConfig(engine)
        nonaConfig.setDefaults(mapOf("key1" to "default"))

        assertEquals("default", nonaConfig.getString("key1"))
        assertTrue(nonaConfig.fetch(), "Fetch should succeed")
        assertEquals("default", nonaConfig.getString("key1"), "Value should not change before activate")
        assertTrue(nonaConfig.activate(), "Activate should succeed")
        assertEquals("remote", nonaConfig.getString("key1"), "Value should change after activate")
    }

    @Test
    fun testFetchThrottling() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = "{}", 
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val nonaConfig = createTestConfig(engine)
        nonaConfig.setConfigSettings(
            NonaConfigSettings.Builder()
                .setMinimumFetchInterval(10.seconds)
                .build()
        )

        // First fetch (lastFetchTime is 0, so it should always succeed if now > 10s)
        // Since now is epoch, this should work.
        assertTrue(nonaConfig.fetch(), "First fetch should succeed")

        // Second fetch immediately should be throttled
        assertFalse(nonaConfig.fetch(), "Second fetch should be throttled")
    }

    @Test
    fun testFetchAndActivateCombined() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = "{\"k\": \"v\"}", 
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val nonaConfig = createTestConfig(engine)
        
        assertTrue(nonaConfig.fetchAndActivate())
        assertEquals("v", nonaConfig.getString("k"))
    }

    @Test
    fun testJsonRetrieval() {
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        val nonaConfig = createTestConfig(engine)
        nonaConfig.setDefaults(mapOf("j" to "{\"key\":\"val\",\"value\":123}"))
        
        val data = nonaConfig.getJson("j", TestJson.serializer())
        assertNotNull(data)
        assertEquals("val", data.key)
        assertEquals(123, data.value)
    }

    @Test
    fun testNonaConfigValue() {
        val v = NonaConfigValueImpl("123")
        assertEquals("123", v.asString())
        assertEquals(123L, v.asLong())
        assertEquals(123.0, v.asDouble())
        assertEquals(false, v.asBoolean())
        
        val v2 = NonaConfigValueImpl("123.45")
        assertEquals(123.45, v2.asDouble())
        assertEquals(0L, v2.asLong()) // expected behavior of toLongOrNull
        
        assertTrue(NonaConfigValueImpl("true").asBoolean())
        assertTrue(NonaConfigValueImpl("1").asBoolean())
    }
}
