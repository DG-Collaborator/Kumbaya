package com.app.backendplug_kmp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.backendplug_kmp.rag.SourceCandidate

/**
   * Catalog search screen. Topic → ranked dataset list → tap to view table + ask questions.
   * Web search tab removed; Q&A now lives on the TableScreen against the loaded dataset.
*/
@Composable
fun AskScreen(
    state: AskState,
    onSearch: (String) -> Unit,
    onOpen: (SourceCandidate) -> Unit,
    onBackToResults: () -> Unit,
    onAsk: (String) -> Unit,
    onBrowse: () -> Unit
) {
    if (state is AskState.Viewing) {
        TableScreen(
            table = state.table,
            answer = state.answer,
            answerLoading = state.answerLoading,
            onAsk = onAsk,
            onBack = onBackToResults
        )
        return
    }

    var query by remember { mutableStateOf("") }
    val busy = state is AskState.Searching || state is AskState.Loading

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Find a dataset", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Search public open-data catalogs by topic",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Topic") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            Button(onClick = { onSearch(query) }, enabled = !busy) {
                Text("Search")
            }
        }
        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when (state) {
                is AskState.Searching, is AskState.Loading -> CircularProgressIndicator()

                is AskState.Results -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.candidates) { candidate ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(candidate) }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(candidate.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                candidate.description,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        HorizontalDivider()
                    }
                }

                is AskState.Error -> Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )

                else -> Unit
            }
        }

        TextButton(onClick = onBrowse) { Text("Browse a source instead →") }
    }
}

