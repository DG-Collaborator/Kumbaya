package com.app.backendplug_kmp

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import platform.Foundation.NSBundle

actual fun openAiApiKey(): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("OPENAI_API_KEY") as? String
        ?: error("OPENAI_API_KEY not set in Info.plist")

actual fun censusApiKey(): String? = null

@Composable
actual fun HorizontalScrollBar(state: ScrollState, modifier: Modifier) {}