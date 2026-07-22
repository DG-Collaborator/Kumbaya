package com.app.backendplug_kmp

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect fun openAiApiKey(): String

expect fun censusApiKey(): String?


@Composable
expect fun HorizontalScrollBar(state: ScrollState, modifier: Modifier = Modifier)