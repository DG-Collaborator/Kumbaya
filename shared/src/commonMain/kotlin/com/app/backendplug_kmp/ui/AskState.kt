package com.app.backendplug_kmp.ui

import com.app.backendplug_kmp.core.domain.DataTable
import com.app.backendplug_kmp.rag.SourceCandidate

/**
   * States for the discover-and-browse flow.
   * Catalog search → Results → Viewing (with inline RAG Q&A on the loaded table).
*/
sealed interface AskState {
    data object Idle : AskState
    data object Searching : AskState
    data class Results(val query: String, val candidates: List<SourceCandidate>) : AskState
    data object Loading : AskState
    data class Viewing(
        val sourceName: String,
        val table: DataTable,
        val answer: String? = null,
        val answerLoading: Boolean = false
    ) : AskState
    data class Error(val message: String) : AskState
}