package com.nonaconfig

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController(apiKey: String) = ComposeUIViewController {
    App(apiKey = apiKey)
}
