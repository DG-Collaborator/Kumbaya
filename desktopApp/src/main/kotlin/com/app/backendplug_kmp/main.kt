package com.app.backendplug_kmp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "BackendPlug_kmp",
    ) {
        App()
    }
}
