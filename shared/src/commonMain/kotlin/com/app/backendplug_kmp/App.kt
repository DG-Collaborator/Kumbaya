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
import com.app.backendplug_kmp.rag.clients.OpenAiClient
import com.app.backendplug_kmp.rag.clients.WebSearchClient
import com.app.backendplug_kmp.ui.AskScreen
import com.app.backendplug_kmp.ui.AskViewModel
import com.app.backendplug_kmp.ui.BackendViewModel
import com.app.backendplug_kmp.ui.TableScreen
import com.app.backendplug_kmp.ui.UiState
import com.app.backendplug_kmp.ui.WelcomeScreen

/**
    * Composition root. The Ask screen (discovery + RAG) is the landing screen;
    * the original browse flow is still reachable via a link. Both view models are
    * wired to concrete sources here, the one place that names them.
*/
@Composable
@Preview
fun App(tavilyKey: String = "") {
    /*
       one HTTP client shared by the JSON source and the resolvers.
       NOTE FOR LATER: view models via remember, so onCleared() never fires;
       fine for a single long-lived window, revisit with real navigation.
    */
    val httpClient = remember { createHttpClient() }

    val browseViewModel = remember { BackendViewModel(JsonTableSource(httpClient)) }
    val askViewModel = remember {

        /*            ***** WEB SEARCH SWAP POINT *****
           Replace WebSearchClient with a DataLakeClient at the same call site when the data lake is ready.
           Replace OpenAiClient with OllamaClient(httpClient) to run generation locally instead.
        */
        val llm: LlmClient = OllamaClient(httpClient, baseUrl = ollamaBaseUrl())  // Having issue with OpenAi client and token usage. Will revisit.

        AskViewModel(
            source = JsonTableSource(httpClient),
            resolver = CompositeCatalogResolver(listOf(
                "Socrata"    to CatalogSourceResolver(httpClient, resultLimit = 30),
                "ArcGIS Hub" to ArcGisCatalogResolver(httpClient, resultLimit = 30),
                "data.gov"   to DataGovCatalogResolver(httpClient, resultLimit = 30)
            )),
            webSearch = WebSearchClient(httpClient, apiKey = tavilyKey),
            llm = llm
        )
    }

    // simple top-level switch between the two demos; Ask is the landing screen
    var showAsk by remember { mutableStateOf(true) }

    MaterialTheme {
        if (showAsk) {
            AskScreen(
                state = askViewModel.uiState,
                mode = askViewModel.mode,
                onSearch = askViewModel::search,
                onModeChange = askViewModel::switchMode,
                onOpen = askViewModel::open,
                onBackToResults = askViewModel::backToResults,
                onBrowse = { showAsk = false }
            )
        } else {
            Column(Modifier.fillMaxSize()) {
                // lightweight way back to the Ask screen
                TextButton(onClick = { showAsk = true }) { Text("← Ask") }
                when (val state = browseViewModel.uiState) {
                    is UiState.Success -> TableScreen(table = state.table, onBack = browseViewModel::reset)
                    else -> WelcomeScreen(state = state, onLoad = browseViewModel::load)
                }
            }
        }
    }
}
