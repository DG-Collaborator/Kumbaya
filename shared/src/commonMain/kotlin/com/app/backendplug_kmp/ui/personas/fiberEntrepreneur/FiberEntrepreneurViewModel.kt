package com.app.backendplug_kmp.ui.personas.fiberEntrepreneur

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.backendplug_kmp.core.source.DataSource
import com.app.backendplug_kmp.rag.clients.LlmClient
import com.app.backendplug_kmp.rag.personas.fiberEntrepreneur.CensusDemographicsClient
import com.app.backendplug_kmp.rag.personas.fiberEntrepreneur.MarketOpportunityPipeline
import com.app.backendplug_kmp.rag.personas.fiberEntrepreneur.MarketOpportunityReport
import kotlinx.coroutines.launch

sealed interface FiberEntrepreneurState {
    data object Idle : FiberEntrepreneurState
    data object Loading : FiberEntrepreneurState
    data class Success(val report: MarketOpportunityReport) : FiberEntrepreneurState
    data class Error(val message: String) : FiberEntrepreneurState
}

/**
    * Drives the Fiber Entrepreneur dashboard screen. Reuses the exact same
    * MarketOpportunityPipeline the MCP tool calls — one pipeline, two
    * consumers (MCP text tool, this in-app dashboard).
*/
class FiberEntrepreneurViewModel(
    source: DataSource,
    llm: LlmClient,
    censusClient: CensusDemographicsClient? = null
) : ViewModel() {

    private val pipeline = MarketOpportunityPipeline(source, llm, censusClient)

    var uiState by mutableStateOf<FiberEntrepreneurState>(FiberEntrepreneurState.Idle)
        private set

    fun analyze(location: String, stateFips: String, countyFips: String, question: String?) {
        if (stateFips.isBlank() || countyFips.isBlank()) {
            uiState = FiberEntrepreneurState.Error("Enter both a state FIPS and county FIPS code.")
            return
        }
        uiState = FiberEntrepreneurState.Loading
        viewModelScope.launch {
            uiState = try {
                FiberEntrepreneurState.Success(pipeline.run(location, stateFips, countyFips, question))
            } catch (e: Exception) {
                FiberEntrepreneurState.Error(e.message ?: "Analysis failed.")
            }
        }
    }

    fun reset() {
        uiState = FiberEntrepreneurState.Idle
    }
}
