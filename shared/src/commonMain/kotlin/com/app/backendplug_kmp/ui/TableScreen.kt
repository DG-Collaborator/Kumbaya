package com.app.backendplug_kmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.backendplug_kmp.core.domain.DataTable

/**
    * Renders a DataTable exactly as the source laid it out: the inferred
    * columns become the header, each row becomes a line.

    * It knows nothing about the data's meaning, give it freeway records or
    * blog posts and it draws both the same way. That generality is the point.
*/
@Composable
fun TableScreen(
    table: DataTable,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // header bar with a route back to the welcome input
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← New source") }
            Spacer(Modifier.width(12.dp))
            Text(
                "${table.rows.size} rows · ${table.columns.size} columns",
                style = MaterialTheme.typography.bodySmall
            )
        }
        HorizontalDivider()

        //One shared horizontal scroll so the header and every row move together
        val horizontal = rememberScrollState()
        val cellWidth = 180.dp

        Column(Modifier.fillMaxSize().horizontalScroll(horizontal)) {
            // header row
            Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
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

            /*
               body rows, lazy so large datasets (100+ posts) stay smooth.
               weight(1f) gives the list the leftover vertical space.
            */
            LazyColumn(Modifier.weight(1f)) {
                items(table.rows) { row ->
                    Row {
                        table.columns.forEach { col ->
                            Text(
                                text = row.value(col.key),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(cellWidth).padding(12.dp)
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
