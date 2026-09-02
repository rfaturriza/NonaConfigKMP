package com.nonaconfig

import androidx.compose.ui.window.ComposeViewController

fun MainViewController(apiKey: String) = ComposeViewController {
    App(apiKey = apiKey)
}
