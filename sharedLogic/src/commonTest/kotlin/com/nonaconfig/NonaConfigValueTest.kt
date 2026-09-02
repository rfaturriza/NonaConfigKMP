package com.nonaconfig

import kotlinx.serialization.Serializable
import kotlin.test.*

@Serializable
data class TestData(val name: String)

class NonaConfigValueTest {

    @Test
    fun testStringConversion() {
        val value = NonaConfigValueImpl("hello")
        assertEquals("hello", value.asString())
    }

    @Test
    fun testBooleanConversion() {
        assertTrue(NonaConfigValueImpl("true").asBoolean())
        assertTrue(NonaConfigValueImpl("TRUE").asBoolean())
        assertTrue(NonaConfigValueImpl("1").asBoolean())
        assertFalse(NonaConfigValueImpl("false").asBoolean())
        assertFalse(NonaConfigValueImpl("0").asBoolean())
        assertFalse(NonaConfigValueImpl("random").asBoolean())
    }

    @Test
    fun testLongConversion() {
        assertEquals(123L, NonaConfigValueImpl("123").asLong())
        assertEquals(0L, NonaConfigValueImpl("abc").asLong())
    }

    @Test
    fun testDoubleConversion() {
        assertEquals(1.23, NonaConfigValueImpl("1.23").asDouble())
        assertEquals(0.0, NonaConfigValueImpl("abc").asDouble())
    }

    @Test
    fun testByteArrayConversion() {
        val text = "hello"
        val value = NonaConfigValueImpl(text)
        assertContentEquals(text.encodeToByteArray(), value.asByteArray())
    }

    @Test
    fun testJsonConversion() {
        val json = "{\"name\":\"test\"}"
        val value = NonaConfigValueImpl(json)
        val data = value.asJson(TestData.serializer())
        assertNotNull(data)
        assertEquals("test", data.name)
    }

    @Test
    fun testInvalidJsonConversion() {
        val value = NonaConfigValueImpl("invalid")
        val data = value.asJson(TestData.serializer())
        assertNull(data)
    }
}
