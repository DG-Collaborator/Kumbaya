package com.app.backendplug_kmp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
    * The landing screen: one input that points the backend at a source.

    * This is the thin, replaceable layer, swap the copy, branding,
    * or extra fields for any use case. It owns only the text being typed; the
    * loading/error feedback is driven entirely by the shared UiState.
*/
@Composable
fun WelcomeScreen(
    state: UiState,
    onLoad: (String) -> Unit
) {
    // the in-progress URL text is the one piece of state this screen owns
    var url by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("BackendPlug", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Point me at a source and I'll show its table.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Source URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { onLoad(url) },
            // block double-submits while a fetch is running
            enabled = state !is UiState.Loading
        ) {
            Text("Load")
        }

        Spacer(Modifier.height(16.dp))

        // inline feedback, just a projection of UiState
        when (state) {
            is UiState.Loading -> CircularProgressIndicator()
            is UiState.Error -> Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            else -> Unit
        }
    }
}