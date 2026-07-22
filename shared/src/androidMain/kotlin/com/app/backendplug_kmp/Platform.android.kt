package com.app.backendplug_kmp

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/*
   The androidLibrary KMP plugin doesn't support BuildConfig (variant-agnostic).
   MainActivity, in the androidApp module, has a real BuildConfig with the key;
   it sets this holder before App() composes.
*/
object AndroidSecrets {
    var openAiApiKey: String = ""
}

actual fun openAiApiKey(): String =
    AndroidSecrets.openAiApiKey.ifBlank {
        error("OPENAI_API_KEY not set — MainActivity must set AndroidSecrets.openAiApiKey before App() runs.")
    }

actual fun censusApiKey(): String? = null

@Composable
actual fun HorizontalScrollBar(state: ScrollState, modifier: Modifier) {}