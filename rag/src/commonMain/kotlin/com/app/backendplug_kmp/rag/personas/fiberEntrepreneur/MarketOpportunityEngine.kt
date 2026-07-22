package com.app.backendplug_kmp.rag.personas.fiberEntrepreneur

import com.app.backendplug_kmp.core.domain.DataTable

/**
    * Deterministic scoring for the Fiber Entrepreneur's Market Opportunity
    * report. Real arithmetic, no LLM involved — an investment-facing opportunity
    * score should never be something a language model guesses.
*/
object MarketOpportunityEngine {

    // FCC Form 477 technology codes (public reference)
    private const val TECH_CODE_FIBER = "50"

    data class GapAnalysis(
        val recordCount: Int,
        val distinctProviders: Int,
        val distinctBlocks: Int,
        val fiberProviderCount: Int,
        val maxAdvertisedDownMbps: Double,
        val hasExistingFiber: Boolean
    )

    data class Demographics(
        val population: Long?,
        val medianHouseholdIncomeUsd: Long?,
        val populationGrowthPercent: Double?
    )

    fun analyzeGap(broadbandData: DataTable): GapAnalysis {
        if (broadbandData.isEmpty) {
            return GapAnalysis(0, 0, 0, 0, 0.0, false)
        }

        val providerIds = mutableSetOf<String>()
        val blockCodes = mutableSetOf<String>()
        val fiberProviderIds = mutableSetOf<String>()
        var maxDown = 0.0

        for (row in broadbandData.rows) {
            val providerId = row.value("provider_id")
            val blockCode = row.value("blockcode")
            val techCode = row.value("techcode")
            val down = row.value("maxaddown").toDoubleOrNull() ?: 0.0

            if (providerId.isNotBlank()) providerIds += providerId
            if (blockCode.isNotBlank()) blockCodes += blockCode
            if (techCode == TECH_CODE_FIBER && providerId.isNotBlank()) fiberProviderIds += providerId
            if (down > maxDown) maxDown = down
        }

        return GapAnalysis(
            recordCount = broadbandData.rows.size,
            distinctProviders = providerIds.size,
            distinctBlocks = blockCodes.size,
            fiberProviderCount = fiberProviderIds.size,
            maxAdvertisedDownMbps = maxDown,
            hasExistingFiber = fiberProviderIds.isNotEmpty()
        )
    }

    /**
        * Percent change between two population counts, or null if either value
        * is missing or the earlier value is non-positive (avoids division by zero
        * and nonsensical percentages).
    */
    fun computeGrowth(earlierPopulation: Long?, laterPopulation: Long?): Double? {
        if (earlierPopulation == null || laterPopulation == null || earlierPopulation <= 0) return null
        return ((laterPopulation - earlierPopulation).toDouble() / earlierPopulation) * 100.0
    }

    /**
        * 0-100 opportunity score. Broadband-gap factors (competition, provider
        * count, top advertised speed) always apply; demographic factors
        * (population growth, income) apply only when Census data was available
        * — CENSUS_API_KEY is optional, so this degrades gracefully rather than
        * failing.

        * Weights below are placeholder constants, tuned for plausibility, not a
        * calibrated financial model — this proves the "deterministic math; treat the exact weights as adjustable.
    */
    fun scoreOpportunity(gap: GapAnalysis, demographics: Demographics? = null): Int {
        if (gap.recordCount == 0) return 0

        val competitionPenalty = (gap.fiberProviderCount * 25).coerceAtMost(50)
        val providerPenalty = (gap.distinctProviders * 5).coerceAtMost(30)
        val speedPenalty = when {
            gap.maxAdvertisedDownMbps >= 1000 -> 20
            gap.maxAdvertisedDownMbps >= 100  -> 10
            else -> 0
        }
        var score = 100 - competitionPenalty - providerPenalty - speedPenalty

        demographics?.populationGrowthPercent?.let { growth ->
            score += when {
                growth >= 10 -> 10
                growth >= 3  -> 5
                growth < 0   -> -10
                else -> 0
            }
        }
        demographics?.medianHouseholdIncomeUsd?.let { income ->
            score += when {
                income >= 100_000 -> 10
                income >= 70_000  -> 5
                else -> 0
            }
        }

        return score.coerceIn(0, 100)
    }
}