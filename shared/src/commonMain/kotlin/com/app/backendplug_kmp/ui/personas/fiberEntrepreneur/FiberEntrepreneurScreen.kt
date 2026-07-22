package com.app.backendplug_kmp.ui.personas.fiberEntrepreneur

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.app.backendplug_kmp.rag.personas.fiberEntrepreneur.DemographicsStatus
import com.app.backendplug_kmp.rag.personas.fiberEntrepreneur.MarketOpportunityReport
import kotlin.math.round

@Composable
fun FiberEntrepreneurScreen(
    state: FiberEntrepreneurState,
    onAnalyze: (location: String, stateFips: String, countyFips: String, question: String?) -> Unit,
    onBack: () -> Unit
) {
    var location by remember { mutableStateOf("") }
    var stateFips by remember { mutableStateOf("") }
    var countyFips by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    val busy = state is FiberEntrepreneurState.Loading

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
            Spacer(Modifier.width(12.dp))
            Text("Fiber Entrepreneur — Market Opportunity", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location (display name)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = stateFips,
                onValueChange = { stateFips = it },
                label = { Text("State FIPS (2 digits)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = countyFips,
                onValueChange = { countyFips = it },
                label = { Text("County FIPS (3 digits)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text("Focus question (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onAnalyze(location, stateFips, countyFips, question.ifBlank { null }) },
            enabled = !busy
        ) {
            Text("Analyze")
        }

        Spacer(Modifier.height(20.dp))

        when (state) {
            is FiberEntrepreneurState.Idle -> Text(
                "Enter a location's FIPS codes and tap Analyze.",
                style = MaterialTheme.typography.bodyMedium
            )
            is FiberEntrepreneurState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is FiberEntrepreneurState.Error -> Text(
                state.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            is FiberEntrepreneurState.Success -> ReportView(state.report)
        }
    }
}

@Composable
private fun ReportView(report: MarketOpportunityReport) {
    Column(Modifier.fillMaxWidth()) {
        Text(report.location, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        ScoreTile(report.score)
        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatPanel(
                title = "Broadband Gap",
                modifier = Modifier.weight(1f),
                rows = listOf(
                    "Providers" to "${report.gap.distinctProviders}",
                    "Fiber providers" to "${report.gap.fiberProviderCount}",
                    "Max speed" to "${report.gap.maxAdvertisedDownMbps} Mbps",
                    "Existing fiber" to if (report.gap.hasExistingFiber) "Yes" else "No"
                )
            )
            StatPanel(
                title = "Demographics",
                modifier = Modifier.weight(1f),
                rows = when (report.demographicsStatus) {
                    DemographicsStatus.NOT_CONFIGURED -> listOf("Status" to "CENSUS_API_KEY not configured")
                    DemographicsStatus.FETCH_FAILED -> listOf("Status" to "Fetch failed: ${report.demographicsError ?: "unknown"}")
                    DemographicsStatus.AVAILABLE -> listOf(
                        "Population" to "${report.demographics?.population ?: "unavailable"}",
                        "Median income" to (report.demographics?.medianHouseholdIncomeUsd?.let { "$$it" } ?: "unavailable"),
                        "Pop. growth (2018→2022)" to (report.demographics?.populationGrowthPercent?.let { "${round(it * 10) / 10.0}%" } ?: "unavailable")
                    )
                }
            )
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("Narrative Report", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(report.narrative, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ScoreTile(score: Int) {
    val color = when {
        score >= 70 -> MaterialTheme.colorScheme.primary
        score >= 40 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Box(
        Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Fiber Opportunity Score: $score/100",
            style = MaterialTheme.typography.headlineSmall,
            color = color
        )
    }
}

@Composable
private fun StatPanel(title: String, rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        rows.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.bodySmall)
                Text(value, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}