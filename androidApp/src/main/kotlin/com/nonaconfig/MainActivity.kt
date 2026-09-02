package com.nonaconfig

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(apiKey = BuildConfig.NONA_API_KEY)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(apiKey = "BuildConfig.NONA_API_KEY")
}