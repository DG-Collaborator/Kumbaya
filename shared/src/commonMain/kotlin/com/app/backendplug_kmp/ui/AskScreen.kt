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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
    * The Ask screen in two modes:
    *   Catalog — search open-data catalogs, pick a dataset, view its table.
    *   Web     — ask a question, get an LLM answer grounded in live web results.

    * Mode is controlled by the tab row at the top; everything below it adapts
    * to whichever mode is active. AskViewModel owns the mode state.
*/
@Composable
fun AskScreen(
    state: AskState,
    mode: AskState.Mode,
    onSearch: (String) -> Unit,
    onModeChange: (AskState.Mode) -> Unit,
    onOpen: (SourceCandidate) -> Unit,
    onBackToResults: () -> Unit,
    onBrowse: () -> Unit
) {
    if (state is AskState.Viewing) {
        TableScreen(table = state.table, onBack = onBackToResults)
        return
    }

    var query by remember { mutableStateOf("") }
    val busy = state is AskState.Searching || state is AskState.Loading

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Find a dataset", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        // mode toggle, switches between catalog search and web Q&A
        TabRow(selectedTabIndex = mode.ordinal) {
            Tab(
                selected = mode == AskState.Mode.Catalog,
                onClick = { onModeChange(AskState.Mode.Catalog) },
                text = { Text("Catalog") }
            )
            Tab(
                selected = mode == AskState.Mode.Web,
                onClick = { onModeChange(AskState.Mode.Web) },
                text = { Text("Ask the Web") }
            )
        }
        Spacer(Modifier.height(16.dp))

        Text(
            if (mode == AskState.Mode.Catalog)
                "Search public open-data catalogs by topic"
            else
                "Ask any question, the answer is grounded in live web search results.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = {
                    Text(
                        if (mode == AskState.Mode.Catalog) "Topic (e.g. \"wildlife\")"
                        else "Question (e.g. \"Who plays in the World Cup today?\")"
                    )
                },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            Button(onClick = { onSearch(query) }, enabled = !busy) {
                Text(if (mode == AskState.Mode.Catalog) "Search" else "Ask")
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

                // web answer, scrollable in case the response is long
                is AskState.WebAnswer -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(state.question, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(state.answer, style = MaterialTheme.typography.bodyMedium)
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
