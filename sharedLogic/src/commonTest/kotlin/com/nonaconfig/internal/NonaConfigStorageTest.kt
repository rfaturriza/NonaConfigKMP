package com.nonaconfig.internal

import com.russhwolf.settings.MapSettings
import kotlin.test.*

class NonaConfigStorageTest {

    private lateinit var storage: NonaConfigStorage

    @BeforeTest
    fun setup() {
        storage = NonaConfigStorage(MapSettings())
    }

    @Test
    fun testLastFetchTime() {
        assertEquals(0L, storage.lastFetchTime)
        storage.lastFetchTime = 1000L
        assertEquals(1000L, storage.lastFetchTime)
    }

    @Test
    fun testETag() {
        assertNull(storage.eTag)
        storage.eTag = "v1"
        assertEquals("v1", storage.eTag)
        storage.eTag = null
        assertNull(storage.eTag)
    }

    @Test
    fun testFetchedConfig() {
        val config = mapOf("key" to "value")
        storage.saveFetchedConfig(config)
        assertEquals(config, storage.getFetchedConfig())
    }

    @Test
    fun testActiveConfig() {
        val config = mapOf("key" to "value")
        storage.saveActiveConfig(config)
        assertEquals(config, storage.getActiveConfig())
    }

    @Test
    fun testDefaults() {
        val defaults = mapOf("key" to "value")
        storage.saveDefaults(defaults)
        assertEquals(defaults, storage.getDefaults())
    }

    @Test
    fun testInvalidJsonFallback() {
        // Manually corrupting settings to test exception handling
        val settings = MapSettings()
        settings.putString("nona_fetched_config", "{invalid json")
        val corruptedStorage = NonaConfigStorage(settings)
        assertEquals(emptyMap(), corruptedStorage.getFetchedConfig())
    }
}
