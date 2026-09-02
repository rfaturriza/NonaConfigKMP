package com.nonaconfig

import com.nonaconfig.internal.NonaConfigFetcher
import com.nonaconfig.internal.NonaConfigStorage
import com.nonaconfig.internal.currentTimeMillis
import com.russhwolf.settings.Settings
import io.ktor.client.*
import kotlinx.coroutines.*
import kotlinx.serialization.KSerializer

class NonaConfig internal constructor() {

    private lateinit var apiKey: String
    private lateinit var environmentId: String
    private lateinit var storage: NonaConfigStorage
    private lateinit var fetcher: NonaConfigFetcher
    private var settings: NonaConfigSettings = NonaConfigSettings.Builder().build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun initialize(apiKey: String, environmentId: String) {
        this.apiKey = apiKey
        this.environmentId = environmentId
        this.storage = NonaConfigStorage(Settings())
        this.fetcher = NonaConfigFetcher(apiKey, environmentId)
    }

    internal fun initializeForTest(
        apiKey: String,
        environmentId: String,
        settings: Settings,
        httpClient: HttpClient
    ) {
        this.apiKey = apiKey
        this.environmentId = environmentId
        this.storage = NonaConfigStorage(settings)
        this.fetcher = NonaConfigFetcher(apiKey, environmentId, httpClient = httpClient)
    }

    fun setConfigSettings(settings: NonaConfigSettings) {
        this.settings = settings
    }

    fun setDefaults(defaults: Map<String, Any>) {
        val stringDefaults = defaults.mapValues { it.value.toString() }
        storage.saveDefaults(stringDefaults)
    }

    suspend fun fetch(): Boolean = withContext(Dispatchers.Default) {
        val now = currentTimeMillis()
        val lastFetch = storage.lastFetchTime
        
        val interval = settings.minimumFetchInterval.inWholeMilliseconds
        if (now - lastFetch < interval) {
            return@withContext false
        }

        when (val result = fetcher.fetchAll(storage.eTag, settings.releaseVersion)) {
            is NonaConfigFetcher.FetchResult.Success -> {
                storage.saveFetchedConfig(result.config)
                storage.eTag = result.eTag
                storage.lastFetchTime = now
                true
            }
            is NonaConfigFetcher.FetchResult.NotModified -> {
                storage.lastFetchTime = now
                true
            }
            is NonaConfigFetcher.FetchResult.Error -> {
                false
            }
        }
    }

    fun activate(): Boolean {
        val fetched = storage.getFetchedConfig()
        if (fetched.isEmpty()) return false
        storage.saveActiveConfig(fetched)
        return true
    }

    suspend fun fetchAndActivate(): Boolean {
        val fetched = fetch()
        return if (fetched) {
            activate()
        } else {
            false
        }
    }

    fun getValue(key: String): NonaConfigValue {
        val activeConfig = storage.getActiveConfig()
        val value = activeConfig[key] 
            ?: storage.getDefaults()[key]
            ?: ""
        return NonaConfigValueImpl(value)
    }

    fun getString(key: String): String = getValue(key).asString()
    fun getBoolean(key: String): Boolean = getValue(key).asBoolean()
    fun getLong(key: String): Long = getValue(key).asLong()
    fun getDouble(key: String): Double = getValue(key).asDouble()
    fun <T> getJson(key: String, serializer: KSerializer<T>): T? = getValue(key).asJson(serializer)

    companion object {
        private var _instance: NonaConfig? = null
        val instance: NonaConfig 
            get() {
                if (_instance == null) {
                    _instance = NonaConfig()
                }
                return _instance!!
            }

        internal fun resetInstance() {
            _instance = null
        }
    }
}
