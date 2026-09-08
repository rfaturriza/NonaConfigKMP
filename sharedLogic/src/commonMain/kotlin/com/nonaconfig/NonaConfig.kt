package com.nonaconfig

import com.nonaconfig.internal.NonaConfigFetcher
import com.nonaconfig.internal.NonaConfigStorage
import com.nonaconfig.internal.currentTimeMillis
import com.russhwolf.settings.Settings
import io.ktor.client.*
import kotlinx.coroutines.*
import kotlinx.serialization.KSerializer

import kotlin.native.ObjCName
import kotlin.experimental.ExperimentalObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("NonaConfigClient")
class NonaConfig internal constructor() {

    enum class FetchStatus {
        SUCCESS_NEW_DATA,
        SUCCESS_NOT_MODIFIED,
        THROTTLED,
        ERROR
    }

    private lateinit var apiKey: String
    private lateinit var environmentId: String
    private lateinit var baseUrl: String
    private lateinit var storage: NonaConfigStorage
    private lateinit var fetcher: NonaConfigFetcher
    private var testHttpClient: HttpClient? = null
    private var settings: NonaConfigSettings = NonaConfigSettings.Builder().build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val lastETag: String?
        get() = if (::storage.isInitialized) storage.eTag else null

    fun clearETag() {
        if (::storage.isInitialized) {
            storage.eTag = null
        }
    }

    fun initialize(apiKey: String, environmentId: String, baseUrl: String) {
        this.apiKey = apiKey
        this.environmentId = environmentId
        this.baseUrl = baseUrl
        this.storage = NonaConfigStorage(Settings())
        this.fetcher = NonaConfigFetcher(
            apiKey = apiKey,
            environmentId = environmentId,
            baseUrl = settings.baseUrl ?: baseUrl,
            httpClient = testHttpClient
        )
    }

    internal fun initializeForTest(
        apiKey: String,
        environmentId: String,
        baseUrl: String = "http://localhost",
        settings: Settings,
        httpClient: HttpClient
    ) {
        this.apiKey = apiKey
        this.environmentId = environmentId
        this.baseUrl = baseUrl
        this.testHttpClient = httpClient
        this.storage = NonaConfigStorage(settings)
        this.fetcher = NonaConfigFetcher(
            apiKey = apiKey,
            environmentId = environmentId,
            baseUrl = this.settings.baseUrl ?: baseUrl,
            httpClient = httpClient
        )
    }

    fun setConfigSettings(settings: NonaConfigSettings) {
        this.settings = settings
        if (::apiKey.isInitialized && ::environmentId.isInitialized) {
            val effectiveUrl = settings.baseUrl ?: if (::baseUrl.isInitialized) baseUrl else ""
            this.fetcher = NonaConfigFetcher(
                apiKey = apiKey,
                environmentId = environmentId,
                baseUrl = effectiveUrl,
                httpClient = testHttpClient
            )
        }
    }

    fun setDefaults(defaults: Map<String, Any>) {
        val stringDefaults = defaults.mapValues { it.value.toString() }
        storage.saveDefaults(stringDefaults)
    }

    suspend fun fetchWithStatus(): FetchStatus = withContext(Dispatchers.Default) {
        val now = currentTimeMillis()
        val lastFetch = storage.lastFetchTime
        
        val interval = settings.minimumFetchInterval.inWholeMilliseconds
        if (now - lastFetch < interval) {
            return@withContext FetchStatus.THROTTLED
        }

        when (val result = fetcher.fetchAll(storage.eTag, settings.releaseVersion)) {
            is NonaConfigFetcher.FetchResult.Success -> {
                storage.saveFetchedConfig(result.config)
                storage.eTag = result.eTag
                storage.lastFetchTime = now
                FetchStatus.SUCCESS_NEW_DATA
            }
            is NonaConfigFetcher.FetchResult.NotModified -> {
                storage.lastFetchTime = now
                FetchStatus.SUCCESS_NOT_MODIFIED
            }
            is NonaConfigFetcher.FetchResult.Error -> {
                FetchStatus.ERROR
            }
        }
    }

    suspend fun fetch(): Boolean {
        val status = fetchWithStatus()
        return status == FetchStatus.SUCCESS_NEW_DATA || status == FetchStatus.SUCCESS_NOT_MODIFIED
    }

    fun fetch(onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            val result = fetch()
            onComplete?.invoke(result)
        }
    }

    fun activate(): Boolean {
        val fetched = storage.getFetchedConfig()
        if (fetched.isEmpty()) return false
        storage.saveActiveConfig(fetched)
        return true
    }

    suspend fun fetchAndActivateWithStatus(): FetchStatus {
        val status = fetchWithStatus()
        if (status == FetchStatus.SUCCESS_NEW_DATA) {
            activate()
        }
        return status
    }

    fun fetchAndActivateWithStatus(onComplete: (FetchStatus) -> Unit) {
        scope.launch {
            val status = fetchAndActivateWithStatus()
            onComplete(status)
        }
    }

    suspend fun fetchAndActivate(): Boolean {
        val fetched = fetch()
        return if (fetched) {
            activate()
        } else {
            false
        }
    }

    fun fetchAndActivate(onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            val result = fetchAndActivate()
            onComplete?.invoke(result)
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
