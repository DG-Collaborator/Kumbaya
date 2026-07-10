package com.app.backendplug_kmp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.backendplug_kmp.core.source.DataSource
import com.app.backendplug_kmp.core.source.SourceQuery
import com.app.backendplug_kmp.rag.RagPipeline
import com.app.backendplug_kmp.rag.SourceCandidate
import com.app.backendplug_kmp.rag.SourceResolver
import com.app.backendplug_kmp.rag.clients.LlmClient
import kotlinx.coroutines.launch

/**
    * Drives the discover-and-browse flow.
    * Catalog search → Results → Viewing → optional inline RAG Q&A on the loaded table.
*/
class AskViewModel(
    private val source: DataSource,
    private val resolver: SourceResolver,
    private val llm: LlmClient
) : ViewModel() {

    var uiState by mutableStateOf<AskState>(AskState.Idle)
        private set

    private var lastResults: AskState.Results? = null

    fun search(query: String) {
        val q = query.trim()
        if (q.isEmpty()) {
            uiState = AskState.Error("Enter a topic to search for.")
            return
        }
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

    fun open(candidate: SourceCandidate) {
        uiState = AskState.Loading
        viewModelScope.launch {
            uiState = try {
                val table = source.fetch(SourceQuery(candidate.url))
                if (table.isEmpty) AskState.Error("No tabular data at: ${candidate.name}")
                else AskState.Viewing(sourceName = candidate.name, table = table)
            } catch (e: Exception) {
                AskState.Error(e.message ?: "Failed to load that dataset.")
            }
        }
    }

    fun askAboutTable(question: String) {
        val current = uiState as? AskState.Viewing ?: return
        uiState = current.copy(answerLoading = true, answer = null)
        viewModelScope.launch {
            uiState = try {
                val pipeline = RagPipeline(llm)
                pipeline.ingest(current.table)
                current.copy(answerLoading = false, answer = pipeline.answer(question))
            } catch (e: Exception) {
                current.copy(answerLoading = false, answer = "Error: ${e.message}")
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
