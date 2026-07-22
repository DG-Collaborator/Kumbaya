package com.app.backendplug_kmp.rag.personas.fiberEntrepreneur

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
    * Census ACS 5-year population + median household income for one US county.

    * Handled separately from JsonTableSource on purpose: the Census API returns
    * an array of arrays (a header row, then one data row per geography) rather
    * than an array of objects, which JsonTableSource's generic parser doesn't
    * recognize as tabular (it looks for JsonObject rows). Since we only need two
    * named scalar values out of one row, parsing that shape directly here is
    * simpler and more honest than teaching the universal parser a
    * Census-specific convention.

    * Requires a free key: https://api.census.gov/data/key_signup.html
*/
class CensusDemographicsClient(
    private val client: HttpClient,
    private val apiKey: String
) {
    data class Demographics(
        val population: Long?,
        val medianHouseholdIncomeUsd: Long?
    )

    suspend fun fetch(stateFips: String, countyFips: String, year: Int = 2022): Demographics {
        val url = "https://api.census.gov/data/$year/acs/acs5" +
                "?get=NAME,B19013_001E,B01003_001E" +
                "&for=county:$countyFips&in=state:$stateFips" +
                "&key=$apiKey"

        val httpResponse = client.get(url)
        if (!httpResponse.status.isSuccess()) {
            error("Census ACS request failed (${httpResponse.status.value}) — check CENSUS_API_KEY")
        }

        val root: JsonElement = httpResponse.body()
        val rows = (root as? JsonArray) ?: return Demographics(null, null)
        // row 0 is the header ["NAME","B19013_001E","B01003_001E","state","county"], row 1 is the data
        val dataRow = rows.getOrNull(1) as? JsonArray ?: return Demographics(null, null)

        // Census uses sentinel negative values for suppressed/unavailable estimates
        fun cell(index: Int): Long? =
            (dataRow.getOrNull(index) as? JsonPrimitive)?.content?.toLongOrNull()?.takeIf { it >= 0 }

        return Demographics(
            medianHouseholdIncomeUsd = cell(1),
            population = cell(2)
        )
    }
}