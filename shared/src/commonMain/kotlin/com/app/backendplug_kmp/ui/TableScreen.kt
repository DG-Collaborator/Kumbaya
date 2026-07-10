package com.app.backendplug_kmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.app.backendplug_kmp.HorizontalScrollBar
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
import com.app.backendplug_kmp.core.domain.DataTable

@Composable
fun TableScreen(
    table: DataTable,
    answer: String? = null,
    answerLoading: Boolean = false,
    onAsk: (String) -> Unit = {},
    onBack: () -> Unit
) {
    var question by remember { mutableStateOf("") }
    val hScroll = rememberScrollState()
    val cellWidth = 180.dp

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Back") }
            Spacer(Modifier.width(12.dp))
            Text(
                "${table.rows.size} rows · ${table.columns.size} columns",
                style = MaterialTheme.typography.bodySmall
            )
        }
        HorizontalDivider()

        Row(
            Modifier
                .horizontalScroll(hScroll)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            table.columns.forEach { col ->
                Text(
                    text = col.label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(cellWidth).padding(12.dp)
                )
            }
        }
        HorizontalDivider()

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            table.rows.forEachIndexed { index, row ->
                Row(
                    Modifier
                        .horizontalScroll(hScroll)
                        .background(
                            if (index % 2 == 0) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                ) {
                    table.columns.forEach { col ->
                        Text(
                            text = row.value(col.key),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(cellWidth).padding(12.dp)
                        )
                    }
                }
            }
        }

        HorizontalScrollBar(
            state = hScroll,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Ask a question about this data") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onAsk(question) },
                    enabled = question.isNotBlank() && !answerLoading
                ) {
                    Text("Ask")
                }
            }
            if (answerLoading) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            if (answer != null) {
                Spacer(Modifier.height(8.dp))
                Text(answer, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}