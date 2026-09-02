package com.nonaconfig.internal

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.serialization.json.Json

internal class NonaConfigStorage(private val settings: Settings) {
    private val json = Json { ignoreUnknownKeys = true }

    var lastFetchTime: Long
        get() = settings.getLong(KEY_LAST_FETCH_TIME, 0L)
        set(value) {
            settings[KEY_LAST_FETCH_TIME] = value
        }

    var eTag: String?
        get() {
            val tag = settings.getString(KEY_E_TAG, "")
            return tag.ifEmpty { null }
        }
        set(value) {
            if (value != null) settings[KEY_E_TAG] = value
            else settings.remove(KEY_E_TAG)
        }

    fun saveFetchedConfig(config: Map<String, String>) {
        settings[KEY_FETCHED_CONFIG] = json.encodeToString(config)
    }

    fun getFetchedConfig(): Map<String, String> {
        val jsonString = settings.getString(KEY_FETCHED_CONFIG, "{}")
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveActiveConfig(config: Map<String, String>) {
        settings[KEY_ACTIVE_CONFIG] = json.encodeToString(config)
    }

    fun getActiveConfig(): Map<String, String> {
        val jsonString = settings.getString(KEY_ACTIVE_CONFIG, "{}")
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveDefaults(defaults: Map<String, String>) {
        settings[KEY_DEFAULTS] = json.encodeToString(defaults)
    }

    fun getDefaults(): Map<String, String> {
        val jsonString = settings.getString(KEY_DEFAULTS, "{}")
        return try {
            json.decodeFromString(jsonString)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    companion object {
        private const val KEY_LAST_FETCH_TIME = "nona_last_fetch_time"
        private const val KEY_FETCHED_CONFIG = "nona_fetched_config"
        private const val KEY_ACTIVE_CONFIG = "nona_active_config"
        private const val KEY_DEFAULTS = "nona_defaults"
        private const val KEY_E_TAG = "nona_e_tag"
    }
}
