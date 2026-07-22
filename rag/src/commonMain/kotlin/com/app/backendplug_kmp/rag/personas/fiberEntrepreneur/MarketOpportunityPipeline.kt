package com.app.backendplug_kmp.rag.personas.fiberEntrepreneur

import com.app.backendplug_kmp.core.source.DataSource
import com.app.backendplug_kmp.core.source.SourceQuery
import com.app.backendplug_kmp.rag.clients.LlmClient
import kotlin.math.round

enum class DemographicsStatus { NOT_CONFIGURED, FETCH_FAILED, AVAILABLE }

/**
    * Structured result of a Market Opportunity analysis. Both the MCP tool
    * (flattened via toText()) and the in-app dashboard screen (rendered as
    * score tile + stat panels) consume this same object — one computation,
    * two presentations.
*/
data class MarketOpportunityReport(
    val location: String,
    val stateFips: String,
    val countyFips: String,
    val score: Int,
    val gap: MarketOpportunityEngine.GapAnalysis,
    val demographics: MarketOpportunityEngine.Demographics?,
    val demographicsStatus: DemographicsStatus,
    val demographicsError: String?,
    val narrative: String
)

/**
    * Fiber Entrepreneur, Module 1: Market Opportunity Intelligence.

    * Fetches FCC broadband deployment records for a county and computes a
    * deterministic gap analysis + Fiber Opportunity Score (MarketOpportunityEngine
    * — real arithmetic, never guessed). When a CensusDemographicsClient is
    * supplied (CENSUS_API_KEY configured), also folds in population/income/growth;
    * otherwise degrades gracefully to broadband-only scoring. The LLM's only job
    * is turning already-computed numbers into readable prose; it never sees or
    * embeds the raw broadband rows (there can be thousands per county —
    * RagPipeline's per-row embedding would be slow and pointless when the numbers
    * are already exact).
*/
class MarketOpportunityPipeline(
    private val jsonSource: DataSource,
    private val llm: LlmClient,
    private val censusClient: CensusDemographicsClient? = null
) {
    suspend fun run(location: String, stateFips: String, countyFips: String, question: String?): MarketOpportunityReport {
        val url = FiberEntrepreneurRegistry.broadbandDeploymentUrl(stateFips, countyFips)
        val table = jsonSource.fetch(SourceQuery(address = url))
        val gap = MarketOpportunityEngine.analyzeGap(table)

        val demographicsResult = censusClient?.let { client ->
            runCatching {
                val current = client.fetch(stateFips, countyFips, year = 2022)
                val earlier = client.fetch(stateFips, countyFips, year = 2018)
                MarketOpportunityEngine.Demographics(
                    population = current.population,
                    medianHouseholdIncomeUsd = current.medianHouseholdIncomeUsd,
                    populationGrowthPercent = MarketOpportunityEngine.computeGrowth(earlier.population, current.population)
                )
            }
        }
        val demographics = demographicsResult?.getOrNull()
        val demographicsStatus = when {
            censusClient == null -> DemographicsStatus.NOT_CONFIGURED
            demographics == null -> DemographicsStatus.FETCH_FAILED
            else -> DemographicsStatus.AVAILABLE
        }
        val demographicsError = demographicsResult?.exceptionOrNull()?.message
        val score = MarketOpportunityEngine.scoreOpportunity(gap, demographics)

        val narrative = if (table.isEmpty) {
            "No FCC broadband deployment records found for this location — double-check the state/county FIPS codes."
        } else {
            llm.generate(
                prompt = question
                    ?: "Summarize this fiber broadband market opportunity for an investor in 3-4 short paragraphs, " +
                    "referencing the figures directly. Be specific about whether this looks underserved or " +
                    "well-served, and why.",
                systemPrompt = "You are a fiber broadband market analyst. Use only the figures given below — " +
                        "they were computed deterministically from FCC/Census data, do not recalculate or contradict them.\n\n" +
                        summaryText(location, stateFips, countyFips, score, gap, demographics, demographicsStatus, demographicsError)
            )
        }

        return MarketOpportunityReport(
            location = location,
            stateFips = stateFips,
            countyFips = countyFips,
            score = score,
            gap = gap,
            demographics = demographics,
            demographicsStatus = demographicsStatus,
            demographicsError = demographicsError,
            narrative = narrative
        )
    }
}

/** Flattened text form, used by the MCP tool's plain-text response. */
fun MarketOpportunityReport.toText(): String =
    summaryText(location, stateFips, countyFips, score, gap, demographics, demographicsStatus, demographicsError) +
            "\n" + narrative

private fun summaryText(
    location: String,
    stateFips: String,
    countyFips: String,
    score: Int,
    gap: MarketOpportunityEngine.GapAnalysis,
    demographics: MarketOpportunityEngine.Demographics?,
    demographicsStatus: DemographicsStatus,
    demographicsError: String?
): String = buildString {
    appendLine("Location: $location (FIPS $stateFips$countyFips)")
    appendLine("Fiber Opportunity Score: $score/100")
    appendLine("Broadband records analyzed: ${gap.recordCount}")
    appendLine("Distinct providers reporting coverage: ${gap.distinctProviders}")
    appendLine("Census blocks with reported coverage: ${gap.distinctBlocks}")
    appendLine("Providers already offering fiber (FCC tech code 50): ${gap.fiberProviderCount}")
    appendLine("Highest advertised download speed available: ${gap.maxAdvertisedDownMbps} Mbps")
    appendLine("Existing fiber in area: ${if (gap.hasExistingFiber) "Yes" else "No"}")
    when (demographicsStatus) {
        DemographicsStatus.NOT_CONFIGURED -> appendLine("Demographics: not available (CENSUS_API_KEY not configured)")
        DemographicsStatus.FETCH_FAILED -> appendLine("Demographics: fetch failed (${demographicsError ?: "unknown error"})")
        DemographicsStatus.AVAILABLE -> {
            appendLine("Population: ${demographics?.population ?: "unavailable"}")
            appendLine("Median household income: ${demographics?.medianHouseholdIncomeUsd?.let { "$$it" } ?: "unavailable"}")
            appendLine("Population growth (2018→2022, ACS5): ${demographics?.populationGrowthPercent?.let(::formatPercent) ?: "unavailable"}")
        }
    }
}

private fun formatPercent(value: Double): String {
    val rounded = round(value * 10) / 10.0
    return "$rounded%"
}
