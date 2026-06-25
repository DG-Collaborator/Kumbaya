package com.app.backendplug_kmp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.backendplug_kmp.core.source.DataSource
import com.app.backendplug_kmp.core.source.SourceQuery
import com.app.backendplug_kmp.currentDate
import com.app.backendplug_kmp.rag.SourceCandidate
import com.app.backendplug_kmp.rag.SourceResolver
import com.app.backendplug_kmp.rag.clients.LlmClient
import com.app.backendplug_kmp.rag.clients.WebSearchClient
import kotlinx.coroutines.launch

/**
    * Drives the Ask screen in two modes:
    *   Catalog — searches open-data catalogs and lets the user browse a dataset table.
    *   Web     — searches the web and asks the LLM to answer grounded in the results.

    * Depends only on interfaces (SourceResolver, DataSource, LlmClient) and
    * WebSearchClient, so each can be swapped independently at the composition root.
*/
class AskViewModel(
    private val source: DataSource,
    private val resolver: SourceResolver,
    private val webSearch: WebSearchClient,
    private val llm: LlmClient
) : ViewModel() {

    var uiState by mutableStateOf<AskState>(AskState.Idle)
        private set

    var mode by mutableStateOf(AskState.Mode.Catalog)
        private set

    private var lastResults: AskState.Results? = null

    fun switchMode(newMode: AskState.Mode) {
        mode = newMode
        uiState = AskState.Idle
        lastResults = null
    }

    fun search(query: String) {
        val q = query.trim()
        if (q.isEmpty()) {
            uiState = AskState.Error(
                if (mode == AskState.Mode.Catalog) "Enter a topic to search for."
                else "Enter a question to ask."
            )
            return
        }
        when (mode) {
            AskState.Mode.Catalog -> searchCatalog(q)
            AskState.Mode.Web     -> searchWeb(q)
        }
    }

    private fun searchCatalog(q: String) {
        uiState = AskState.Searching
        viewModelScope.launch {
            uiState = try {
                val candidates = resolver.search(q)
                if (candidates.isEmpty()) AskState.Error("No datasets found for: $q")
                else AskState.Results(q, candidates).also { lastResults = it }
            } catch (e: Exception) {
                AskState.Error(e.message ?: "Search failed.")
            }
        }
    }

    private fun searchWeb(q: String) {
        uiState = AskState.Searching
        viewModelScope.launch {
            uiState = try {
                val context = webSearch.search(q)
                val answer = llm.generate(
                    prompt = """
                      Answer the question using only the web search results below.
                      If the answer is not in the results and is not a date/time question, say you don't know.
  
                      Results:
                      $context

                      Question: $q
                      Answer:
                  """.trimIndent(),
                    systemPrompt = "Today's date and time is ${currentDate()}. This is from the device clock — always use this value for any date or time question. Do not rely on your training data for dates."
                )
                AskState.WebAnswer(question = q, answer = answer)
            } catch (e: Exception) {
                AskState.Error(e.message ?: "Search failed.")
            }
        }
    }

    fun open(candidate: SourceCandidate) {
        uiState = AskState.Loading
        viewModelScope.launch {
            uiState = try {
                val table = source.fetch(SourceQuery(candidate.url))
                if (table.isEmpty) AskState.Error("No tabular data at: ${candidate.name}")
                else AskState.Viewing(candidate.name, table)
            } catch (e: Exception) {
                AskState.Error(e.message ?: "Failed to load that dataset.")
            }
        }
    }

    fun backToResults() {
        uiState = lastResults ?: AskState.Idle
    }

    fun reset() {
        uiState = AskState.Idle
        lastResults = null
    }
}
