package com.app.backendplug_kmp

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
expect fun currentDate(): String

expect fun ollamaBaseUrl(): String

@Composable
expect fun HorizontalScrollBar(state: ScrollState, modifier: Modifier = Modifier)