package com.nonaconfig

interface NonaConfigValue {
    fun asString(): String
    fun asBoolean(): Boolean
    fun asLong(): Long
    fun asDouble(): Double
    fun asByteArray(): ByteArray
}

internal class NonaConfigValueImpl(private val value: String) : NonaConfigValue {
    override fun asString(): String = value
    override fun asBoolean(): Boolean = value.lowercase() == "true" || value == "1"
    override fun asLong(): Long = value.toLongOrNull() ?: 0L
    override fun asDouble(): Double = value.toDoubleOrNull() ?: 0.0
    override fun asByteArray(): ByteArray = value.encodeToByteArray()
}
