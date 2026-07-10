package com.app.backendplug_kmp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.app.backendplug_kmp.core.net.createHttpClient
import com.app.backendplug_kmp.core.source.JsonTableSource
import com.app.backendplug_kmp.rag.sourceResolvers.CatalogSourceResolver
import com.app.backendplug_kmp.rag.sourceResolvers.ArcGisCatalogResolver
import com.app.backendplug_kmp.rag.sourceResolvers.DataGovCatalogResolver
import com.app.backendplug_kmp.rag.sourceResolvers.CompositeCatalogResolver
import com.app.backendplug_kmp.rag.clients.LlmClient
import com.app.backendplug_kmp.rag.clients.OllamaClient
import com.app.backendplug_kmp.ui.AskScreen
import com.app.backendplug_kmp.ui.AskViewModel
import com.app.backendplug_kmp.ui.BackendViewModel
import com.app.backendplug_kmp.ui.TableScreen
import com.app.backendplug_kmp.ui.UiState
import com.app.backendplug_kmp.ui.WelcomeScreen

/**
   * Composition root. The Ask screen is the landing screen.
   * Q&A on loaded datasets uses the RAG pipeline (Ollama must be running).
*/
@Composable
@Preview
fun App() {
    val httpClient = remember { createHttpClient() }

    val browseViewModel = remember { BackendViewModel(JsonTableSource(httpClient)) }
    val askViewModel = remember {
        /*            ***** LLM SWAP POINT *****
           Replace OllamaClient with OpenAiClient(httpClient, apiKey = ...) when ready.
           Same swap activates web RAG and table Q&A.
        */
        val llm: LlmClient = OllamaClient(httpClient, baseUrl = ollamaBaseUrl())

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

    var showAsk by remember { mutableStateOf(true) }

    MaterialTheme {
        if (showAsk) {
            AskScreen(
                state     = askViewModel.uiState,
                onSearch  = askViewModel::search,
                onOpen    = askViewModel::open,
                onBackToResults = askViewModel::backToResults,
                onAsk     = askViewModel::askAboutTable,
                onBrowse  = { showAsk = false }
            )
        } else {
            Column(Modifier.fillMaxSize()) {
                TextButton(onClick = { showAsk = true }) { Text("← Ask") }
                when (val state = browseViewModel.uiState) {
                    is UiState.Success -> TableScreen(table = state.table, onBack = browseViewModel::reset)
                    else -> WelcomeScreen(state = state, onLoad = browseViewModel::load)
                }
            }
        }
    }
}