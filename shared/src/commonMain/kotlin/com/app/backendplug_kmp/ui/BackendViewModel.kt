package com.app.backendplug_kmp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.backendplug_kmp.core.source.DataSource
import com.app.backendplug_kmp.core.source.SourceQuery
import kotlinx.coroutines.launch

/**
    * Drives the UI by turning "load this URL" into UiState transitions.

    * It depends only on the DataSource interface, never on JsonTableSource or
    * any concrete source. That is the swap seam: change what gets injected and
    * this class, plus every screen, stays exactly the same.
*/
class BackendViewModel(
    private val source: DataSource
) : ViewModel() {

    // Compose observes this; assigning it recomposes whatever reads it.
    var uiState by mutableStateOf<UiState>(UiState.Idle)
        private set

    /**
        * Fetch the given source and publish the result as state.
        * Validation, loading, success-but-empty, and failure are all just
        * different UiState values, so the screen never branches on raw data.
    */
    fun load(address: String) {
        val target = address.trim()
        if (target.isEmpty()) {
            uiState = UiState.Error("Enter a source URL first.")
            return
        }

        uiState = UiState.Loading
        viewModelScope.launch {
            uiState = try {
                // wrap the raw text in the neutral descriptor; the injected
                // source decides how to interpret the address
                val table = source.fetch(SourceQuery(target))
                if (table.isEmpty) UiState.Error("No tabular data found at that source.")
                else UiState.Success(table)
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Failed to load the source.")
            }
        }
    }

    // back to the welcome input from a result screen
    fun reset() {
        uiState = UiState.Idle
    }
}