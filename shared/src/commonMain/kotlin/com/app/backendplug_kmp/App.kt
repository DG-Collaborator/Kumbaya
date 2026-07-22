package com.app.backendplug_kmp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.backendplug_kmp.core.net.createHttpClient
import com.app.backendplug_kmp.core.source.JsonTableSource
import com.app.backendplug_kmp.rag.sourceResolvers.CatalogSourceResolver
import com.app.backendplug_kmp.rag.sourceResolvers.ArcGisCatalogResolver
import com.app.backendplug_kmp.rag.sourceResolvers.DataGovCatalogResolver
import com.app.backendplug_kmp.rag.sourceResolvers.CompositeCatalogResolver
import com.app.backendplug_kmp.rag.clients.LlmClient
import com.app.backendplug_kmp.rag.clients.OpenAiClient
import com.app.backendplug_kmp.rag.personas.fiberEntrepreneur.CensusDemographicsClient
import com.app.backendplug_kmp.ui.AskScreen
import com.app.backendplug_kmp.ui.AskViewModel
import com.app.backendplug_kmp.ui.BackendViewModel
import com.app.backendplug_kmp.ui.TableScreen
import com.app.backendplug_kmp.ui.UiState
import com.app.backendplug_kmp.ui.WelcomeScreen
import com.app.backendplug_kmp.ui.personas.fiberEntrepreneur.FiberEntrepreneurScreen
import com.app.backendplug_kmp.ui.personas.fiberEntrepreneur.FiberEntrepreneurViewModel

private enum class AppScreen { Ask, Browse, FiberEntrepreneur }

/**
 * Composition root. Three destinations: Ask (catalog search + RAG Q&A),
 * Browse (typed URL), and the Fiber Entrepreneur dashboard — the first
 * persona-specific screen, following the same seam pattern as the MCP
 * server's persona tools (shared clients built once, handed to each
 * persona's own view model).
 */
@Composable
@Preview
fun App() {
    val httpClient = remember { createHttpClient() }
    val llm: LlmClient = remember { OpenAiClient(httpClient, apiKey = openAiApiKey()) }

    val browseViewModel = remember { BackendViewModel(JsonTableSource(httpClient)) }
    val askViewModel = remember {
        AskViewModel(
            source = JsonTableSource(httpClient),
            resolver = CompositeCatalogResolver(listOf(
                "Socrata"    to CatalogSourceResolver(httpClient, resultLimit = 30),
                "ArcGIS Hub" to ArcGisCatalogResolver(httpClient, resultLimit = 30),
                "data.gov"   to DataGovCatalogResolver(httpClient, resultLimit = 30)
            )),
            llm = llm
        )
    }
    val fiberEntrepreneurViewModel = remember {
        val censusKey = censusApiKey()
        FiberEntrepreneurViewModel(
            source = JsonTableSource(httpClient),
            llm = llm,
            censusClient = censusKey?.takeIf { it.isNotBlank() }?.let { CensusDemographicsClient(httpClient, it) }
        )
    }

    var screen by remember { mutableStateOf(AppScreen.Ask) }

    MaterialTheme {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                TextButton(onClick = { screen = AppScreen.Ask }) { Text("Ask") }
                TextButton(onClick = { screen = AppScreen.Browse }) { Text("Browse") }
                TextButton(onClick = { screen = AppScreen.FiberEntrepreneur }) { Text("Fiber Entrepreneur") }
            }
            HorizontalDivider()

            when (screen) {
                AppScreen.Ask -> AskScreen(
                    state     = askViewModel.uiState,
                    onSearch  = askViewModel::search,
                    onOpen    = askViewModel::open,
                    onBackToResults = askViewModel::backToResults,
                    onAsk     = askViewModel::askAboutTable,
                    onBrowse  = { screen = AppScreen.Browse }
                )
                AppScreen.Browse -> when (val state = browseViewModel.uiState) {
                    is UiState.Success -> TableScreen(table = state.table, onBack = browseViewModel::reset)
                    else -> WelcomeScreen(state = state, onLoad = browseViewModel::load)
                }
                AppScreen.FiberEntrepreneur -> FiberEntrepreneurScreen(
                    state = fiberEntrepreneurViewModel.uiState,
                    onAnalyze = fiberEntrepreneurViewModel::analyze,
                    onBack = { screen = AppScreen.Ask }
                )
            }
        }
    }
}
