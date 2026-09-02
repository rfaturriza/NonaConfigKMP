package com.nonaconfig

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

interface NonaConfigValue {
    fun asString(): String
    fun asBoolean(): Boolean
    fun asLong(): Long
    fun asDouble(): Double
    fun asByteArray(): ByteArray
    fun <T> asJson(serializer: KSerializer<T>): T?
}

internal class NonaConfigValueImpl(
    private val value: String,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : NonaConfigValue {
    override fun asString(): String = value
    override fun asBoolean(): Boolean = value.lowercase() == "true" || value == "1"
    override fun asLong(): Long = value.toLongOrNull() ?: 0L
    override fun asDouble(): Double = value.toDoubleOrNull() ?: 0.0
    override fun asByteArray(): ByteArray = value.encodeToByteArray()
    
    override fun <T> asJson(serializer: KSerializer<T>): T? {
        return try {
            json.decodeFromString(serializer, value)
        } catch (e: Exception) {
            null
        }
    }
}
