package com.nonaconfig

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

@Serializable
data class ThemePalette(
    val primary: String? = null,
    val surface: String? = null,
    val accent: String? = null,
    val radius: Int? = null
)

/**
 * Utility singleton class for handling Remote Config operations using NonaConfig KMP SDK
 */
object RemoteConfigService {

    const val KEY_FREE_SHIPPING_THRESHOLD = "Checkout:FreeShippingThreshold"
    const val KEY_MAX_CART_ITEMS = "Checkout:MaxCartItems"
    const val KEY_FEATURES_CHECKOUT = "Features:Checkout"
    const val KEY_FEATURES_LIVE_CHAT = "Features:LiveChat"
    const val KEY_FEATURES_NEW_NAVIGATION = "Features:NewNavigation"
    const val KEY_KILLSWITCH_PAYMENTS = "Killswitch:Payments"
    const val KEY_SUPPORT_EMAIL = "Support:Email"
    const val KEY_THEME_PALETTE = "Theme:Palette"

    private lateinit var remoteConfig: NonaConfig
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    /**
     * Initialize the Remote Config instance
     */
    fun init(apiKey: String): NonaConfig {
        remoteConfig = getFirebaseRemoteConfig(apiKey)
        return remoteConfig
    }

    /**
     * Get and configure NonaConfig Remote Config instance
     * @return Configured NonaConfig instance
     */
    private fun getFirebaseRemoteConfig(apiKey: String): NonaConfig {
        val nonaConfig = NonaConfig.instance
        nonaConfig.initialize(
            apiKey = apiKey,
            environmentId = "Development",
            baseUrl = "https://demo.nonaconfig.com"
        )

        val configSettings = NonaConfigSettings.Builder()
        configSettings.setMinimumFetchInterval(
            interval = 0.seconds
        )

        nonaConfig.setConfigSettings(configSettings.build())

        nonaConfig.setDefaults(
            mapOf(
                KEY_FREE_SHIPPING_THRESHOLD to 25L,
                KEY_MAX_CART_ITEMS to 50L,
                KEY_FEATURES_CHECKOUT to true,
                KEY_FEATURES_LIVE_CHAT to true,
                KEY_FEATURES_NEW_NAVIGATION to true,
                KEY_KILLSWITCH_PAYMENTS to false,
                KEY_SUPPORT_EMAIL to "DEFAULT-dev-support@acme.example",
                KEY_THEME_PALETTE to "{\"primary\":\"#5b6ef5\",\"surface\":\"#0f1115\",\"accent\":\"#f5a623\",\"radius\":12}"
            )
        )

        scope.launch {
            try {
                nonaConfig.fetchAndActivate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return nonaConfig
    }

    /**
     * Fetch and activate remote configuration asynchronously using [scope].
     * Non-suspend function for convenient invocation from Android, iOS, or Compose UI.
     */
    fun fetchAndActivate(onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            try {
                val success = remoteConfig.fetchAndActivate()
                onComplete?.invoke(success)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete?.invoke(false)
            }
        }
    }

    fun fetchAndActivateWithStatus(onComplete: (NonaConfig.FetchStatus) -> Unit) {
        remoteConfig.fetchAndActivateWithStatus(onComplete)
    }

    fun getETag(): String? = if (::remoteConfig.isInitialized) remoteConfig.lastETag else null

    fun clearETag() {
        if (::remoteConfig.isInitialized) {
            remoteConfig.clearETag()
        }
    }

    // Typed Getters
    fun getFreeShippingThreshold(): Long = remoteConfig.getLong(KEY_FREE_SHIPPING_THRESHOLD)
    fun getMaxCartItems(): Long = remoteConfig.getLong(KEY_MAX_CART_ITEMS)
    fun getFeaturesCheckout(): Boolean = remoteConfig.getBoolean(KEY_FEATURES_CHECKOUT)
    fun getFeaturesLiveChat(): Boolean = remoteConfig.getBoolean(KEY_FEATURES_LIVE_CHAT)
    fun getFeaturesNewNavigation(): Boolean = remoteConfig.getBoolean(KEY_FEATURES_NEW_NAVIGATION)
    fun getKillswitchPayments(): Boolean = remoteConfig.getBoolean(KEY_KILLSWITCH_PAYMENTS)
    fun getSupportEmail(): String = remoteConfig.getString(KEY_SUPPORT_EMAIL)
    fun getThemePalette(): ThemePalette? = remoteConfig.getJson(KEY_THEME_PALETTE, ThemePalette.serializer())
}
