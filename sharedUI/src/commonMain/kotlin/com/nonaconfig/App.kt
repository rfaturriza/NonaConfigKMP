package com.nonaconfig

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource

import nonaconfigkmp.sharedui.generated.resources.Res
import nonaconfigkmp.sharedui.generated.resources.compose_multiplatform

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

@Serializable
data class ThemeConfig(val primaryColor: String, val showBanner: Boolean)

@Composable
@Preview
fun App(apiKey: String) {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        var welcomeMessage by remember { mutableStateOf("Loading...") }
        var themeConfig by remember { mutableStateOf<ThemeConfig?>(null) }

        LaunchedEffect(Unit) {
            val nonaConfig = NonaConfig.instance
            nonaConfig.initialize(apiKey, "production")
            
            // Set complex defaults including JSON
            nonaConfig.setDefaults(mapOf(
                "welcome_message" to "Hello from Defaults!",
                "theme_settings" to "{\"primaryColor\": \"#FF0000\", \"showBanner\": true}"
            ))
            
            welcomeMessage = nonaConfig.getString("welcome_message")
            themeConfig = nonaConfig.getValue("theme_settings").asJson(ThemeConfig.serializer())
        }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = welcomeMessage, style = MaterialTheme.typography.headlineMedium)
            
            themeConfig?.let {
                Text(text = "Primary Color: ${it.primaryColor}")
                if (it.showBanner) {
                    Text(text = "Banner is ON", color = MaterialTheme.colorScheme.error)
                }
            }

            Button(onClick = { showContent = !showContent }) {
                Text("Toggle Image")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
        }
    }
}