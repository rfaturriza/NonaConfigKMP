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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import com.nonaconfig.internal.currentTimeMillis

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
        nonaConfig.initializeForTest("key", "env", "http://localhost", settings, client)
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

    @Test
    fun testPlatform() {
        val platform = getPlatform()
        assertTrue(platform.name.isNotBlank())
    }

    @Test
    fun testCompanionInstance() {
        NonaConfig.resetInstance()
        val inst1 = NonaConfig.instance
        val inst2 = NonaConfig.instance
        assertSame(inst1, inst2)
        NonaConfig.resetInstance()
    }

    @Test
    fun testFetchNotModifiedResult() = runTest {
        val engine = MockEngine { _ ->
            respond(content = "", status = HttpStatusCode.NotModified)
        }
        val nonaConfig = createTestConfig(engine)
        assertTrue(nonaConfig.fetch())
    }

    @Test
    fun testFetchErrorResult() = runTest {
        val engine = MockEngine { _ ->
            respond(content = "Error", status = HttpStatusCode.BadRequest)
        }
        val nonaConfig = createTestConfig(engine)
        assertFalse(nonaConfig.fetch())
    }

    @Test
    fun testActivateEmpty() {
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        val nonaConfig = createTestConfig(engine)
        assertFalse(nonaConfig.activate())
    }

    @Test
    fun testNumericRetrieval() {
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        val nonaConfig = createTestConfig(engine)
        nonaConfig.setDefaults(mapOf("longKey" to 456L, "doubleKey" to 78.9))
        assertEquals(456L, nonaConfig.getLong("longKey"))
        assertEquals(78.9, nonaConfig.getDouble("doubleKey"))
    }

    @Test
    fun testByteArrayOnValue() {
        val value = NonaConfigValueImpl("test")
        assertContentEquals("test".encodeToByteArray(), value.asByteArray())
    }

    @Test
    fun testSettingsBuilder() {
        val settings = NonaConfigSettings.Builder()
            .setMinimumFetchInterval(5.minutes)
            .setFetchTimeout(30.seconds)
            .setReleaseVersion("2.0.0")
            .setBaseUrl("https://custom.nona.io")
            .build()
        assertEquals(5.minutes, settings.minimumFetchInterval)
        assertEquals(30.seconds, settings.fetchTimeout)
        assertEquals("2.0.0", settings.releaseVersion)
        assertEquals("https://custom.nona.io", settings.baseUrl)
    }

    @Test
    fun testRealInitialize() {
        val config = NonaConfig()
        config.initialize("test-key", "test-env", "https://test.nona.io")
        assertNotNull(config)
    }

    @Test
    fun testFetchAndActivateFailure() = runTest {
        val engine = MockEngine { _ ->
            respond(content = "Error", status = HttpStatusCode.InternalServerError)
        }
        val nonaConfig = createTestConfig(engine)
        assertFalse(nonaConfig.fetchAndActivate())
    }

    @Test
    fun testSettingsDefaultBuilder() {
        val settings = NonaConfigSettings.Builder().build()
        assertEquals(12.hours, settings.minimumFetchInterval)
        assertEquals(1.minutes, settings.fetchTimeout)
        assertNull(settings.releaseVersion)
        assertNull(settings.baseUrl)

        val directSettings = NonaConfigSettings(5.minutes, 1.minutes, "1.0", "https://custom.nona.io")
        assertEquals(5.minutes, directSettings.minimumFetchInterval)
        assertEquals(1.minutes, directSettings.fetchTimeout)
        assertEquals("1.0", directSettings.releaseVersion)
        assertEquals("https://custom.nona.io", directSettings.baseUrl)
    }

    @Test
    fun testSetConfigSettingsUpdatesFetcher() = runTest {
        val engine = MockEngine { request ->
            assertTrue(request.url.toString().startsWith("https://custom.nona.io/api/env"))
            respond(
                content = "{\"key\": \"value\"}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = createTestConfig(engine)
        config.setConfigSettings(
            NonaConfigSettings.Builder()
                .setBaseUrl("https://custom.nona.io")
                .setMinimumFetchInterval(0.seconds)
                .build()
        )
        assertTrue(config.fetch())
    }

    @Test
    fun testTimeUtils() {
        assertTrue(currentTimeMillis() > 0)
    }
}
