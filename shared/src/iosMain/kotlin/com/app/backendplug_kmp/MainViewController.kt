package com.app.backendplug_kmp

import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSBundle

fun MainViewController() = ComposeUIViewController {
    val tavilyKey = NSBundle.mainBundle.infoDictionary
        ?.get("TAVILY_API_KEY") as? String ?: ""
    App(tavilyKey = tavilyKey)
}