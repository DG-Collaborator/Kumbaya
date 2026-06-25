package com.app.backendplug_kmp

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
        /*
           Android key source
        */
        setContent {
            App(tavilyKey = BuildConfig.TAVILY_API_KEY)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}