package com.nonaconfig

import com.russhwolf.multiplatform.settings.MapSettings
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class NonaConfigTest {

    @Test
    fun testDefaults() {
        val nonaConfig = NonaConfig.instance
        nonaConfig.initialize("test-api-key", "test-env")
        
        val defaults = mapOf(
            "welcome_message" to "Hello Default",
            "feature_enabled" to true,
            "max_retries" to 5L
        )
        nonaConfig.setDefaults(defaults)

        assertEquals("Hello Default", nonaConfig.getString("welcome_message"))
        assertEquals(true, nonaConfig.getBoolean("feature_enabled"))
        assertEquals(5L, nonaConfig.getLong("max_retries"))
    }

    @Test
    fun testGetValueFallback() {
        val nonaConfig = NonaConfig.instance
        nonaConfig.initialize("test-api-key", "test-env")
        
        assertEquals("", nonaConfig.getString("non_existent_key"))
        assertEquals(false, nonaConfig.getBoolean("non_existent_key"))
        assertEquals(0L, nonaConfig.getLong("non_existent_key"))
    }
}
