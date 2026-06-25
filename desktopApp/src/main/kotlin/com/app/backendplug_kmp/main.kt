package com.app.backendplug_kmp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    val tavilyKey = System.getenv("TAVILY_API_KEY") ?: ""
    val openAiKey = System.getenv("OPENAI_API_KEY") ?: ""
    Window(
        onCloseRequest = ::exitApplication,
        title = "BackendPlug_kmp",
    ) {
        App(tavilyKey = tavilyKey)
    }
}
