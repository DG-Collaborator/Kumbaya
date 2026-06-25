package com.app.backendplug_kmp.ui

import com.app.backendplug_kmp.core.domain.DataTable

/**
    * Every state the welcome/table flow can be in.

    * Kept separate from any source: the shell reacts to these four cases no
    * matter where the DataTable came from.
*/
sealed interface UiState {
    // nothing requested yet, the fresh welcome screen
    data object Idle : UiState

    // a fetch is in flight
    data object Loading : UiState

    // got a table, render it
    data class Success(val table: DataTable) : UiState

    // fetch failed or returned nothing, show why
    data class Error(val message: String) : UiState
}
