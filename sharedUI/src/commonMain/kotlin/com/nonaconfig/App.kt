package com.nonaconfig

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
@Preview
fun App(apiKey: String = "") {
    var isFetching by remember { mutableStateOf(false) }
    var fetchStatus by remember { mutableStateOf("Initializing...") }
    var currentETag by remember { mutableStateOf<String?>(null) }

    // State for Remote Config parameters
    var freeShippingThreshold by remember { mutableLongStateOf(0L) }
    var maxCartItems by remember { mutableLongStateOf(0L) }
    var featureCheckout by remember { mutableStateOf(false) }
    var featureLiveChat by remember { mutableStateOf(false) }
    var featureNewNav by remember { mutableStateOf(false) }
    var killswitchPayments by remember { mutableStateOf(false) }
    var supportEmail by remember { mutableStateOf("") }
    var themePalette by remember { mutableStateOf<ThemePalette?>(null) }

    // Helper function to pull the latest active values & ETag from NonaConfig
    fun updateKeyValues() {
        freeShippingThreshold = RemoteConfigService.getFreeShippingThreshold()
        maxCartItems = RemoteConfigService.getMaxCartItems()
        featureCheckout = RemoteConfigService.getFeaturesCheckout()
        featureLiveChat = RemoteConfigService.getFeaturesLiveChat()
        featureNewNav = RemoteConfigService.getFeaturesNewNavigation()
        killswitchPayments = RemoteConfigService.getKillswitchPayments()
        supportEmail = RemoteConfigService.getSupportEmail()
        themePalette = RemoteConfigService.getThemePalette()
        currentETag = RemoteConfigService.getETag()
    }

    fun performFetchAndActivate() {
        isFetching = true
        RemoteConfigService.fetchAndActivateWithStatus { status ->
            fetchStatus = when (status) {
                NonaConfig.FetchStatus.SUCCESS_NEW_DATA -> "200 OK (Downloaded New Data & ETag Updated)"
                NonaConfig.FetchStatus.SUCCESS_NOT_MODIFIED -> "304 Not Modified (ETag Matched - Using Cached Config)"
                NonaConfig.FetchStatus.THROTTLED -> "Throttled (Minimum Fetch Interval Active)"
                NonaConfig.FetchStatus.ERROR -> "Fetch Error"
            }
            updateKeyValues()
            isFetching = false
        }
    }

    LaunchedEffect(Unit) {
        // Step 1: Initialize SDK & render immediately with cached or default values
        RemoteConfigService.init(apiKey)
        updateKeyValues()
        fetchStatus = "Loaded cached/default values. Checking cloud for updates..."

        // Step 2: Fetch & Activate with ETag handling
        performFetchAndActivate()
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "NonaConfig Remote Parameters",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Status: $fetchStatus",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (isFetching) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Current ETag Header",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentETag ?: "(None - No ETag cached yet)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Active Parameters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    HorizontalDivider()

                    KeyResultItem(
                        keyName = RemoteConfigService.KEY_FREE_SHIPPING_THRESHOLD,
                        value = "$freeShippingThreshold",
                        valueType = "Number"
                    )

                    KeyResultItem(
                        keyName = RemoteConfigService.KEY_MAX_CART_ITEMS,
                        value = "$maxCartItems",
                        valueType = "Number"
                    )

                    KeyResultItem(
                        keyName = RemoteConfigService.KEY_FEATURES_CHECKOUT,
                        value = "$featureCheckout",
                        valueType = "Boolean"
                    )

                    KeyResultItem(
                        keyName = RemoteConfigService.KEY_FEATURES_LIVE_CHAT,
                        value = "$featureLiveChat",
                        valueType = "Boolean"
                    )

                    KeyResultItem(
                        keyName = RemoteConfigService.KEY_FEATURES_NEW_NAVIGATION,
                        value = "$featureNewNav",
                        valueType = "Boolean"
                    )

                    KeyResultItem(
                        keyName = RemoteConfigService.KEY_KILLSWITCH_PAYMENTS,
                        value = "$killswitchPayments",
                        valueType = "Boolean"
                    )

                    KeyResultItem(
                        keyName = RemoteConfigService.KEY_SUPPORT_EMAIL,
                        value = supportEmail,
                        valueType = "Text"
                    )

                    KeyResultItem(
                        keyName = RemoteConfigService.KEY_THEME_PALETTE,
                        value = themePalette?.toString() ?: "(null)",
                        valueType = "JSON Object"
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { performFetchAndActivate() },
                    enabled = !isFetching,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Fetch (Send ETag)")
                }

                OutlinedButton(
                    onClick = {
                        RemoteConfigService.clearETag()
                        performFetchAndActivate()
                    },
                    enabled = !isFetching,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Force Fetch (Clear ETag)")
                }
            }
        }
    }
}

@Composable
private fun KeyResultItem(keyName: String, value: String, valueType: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "$keyName ($valueType)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
