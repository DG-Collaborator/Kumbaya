package com.app.backendplug_kmp

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual fun openAiApiKey(): String =
    System.getenv("OPENAI_API_KEY")
        ?: error("OPENAI_API_KEY environment variable is not set.")

actual fun censusApiKey(): String? =
    System.getenv("CENSUS_API_KEY")

@Composable
actual fun HorizontalScrollBar(state: ScrollState, modifier: Modifier) {
    HorizontalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier
    )
}