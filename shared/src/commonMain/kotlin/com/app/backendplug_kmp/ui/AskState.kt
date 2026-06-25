package com.app.backendplug_kmp.ui

import com.app.backendplug_kmp.core.domain.DataTable
import com.app.backendplug_kmp.rag.SourceCandidate

/**
    * States for the discover-and-browse flow. Mode controls which path the screen
    * is in: Catalog (search open-data catalogs, pick a dataset, view its table)
    * or Web (ask a question, get an LLM answer grounded in live web results).
*/
sealed interface AskState {
    data object Idle : AskState
    data object Searching : AskState
    data class Results(val query: String, val candidates: List<SourceCandidate>) : AskState
    data object Loading : AskState
    data class Viewing(val sourceName: String, val table: DataTable) : AskState
    data class WebAnswer(val question: String, val answer: String) : AskState
    data class Error(val message: String) : AskState

    enum class Mode { Catalog, Web }
}