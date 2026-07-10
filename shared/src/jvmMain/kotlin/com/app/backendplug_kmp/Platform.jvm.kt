package com.app.backendplug_kmp

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun ollamaBaseUrl(): String = "http://localhost:11434"

actual fun currentDate(): String =
    java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy, h:mm a"))

@Composable
actual fun HorizontalScrollBar(state: ScrollState, modifier: Modifier) {
    HorizontalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier
    )
}