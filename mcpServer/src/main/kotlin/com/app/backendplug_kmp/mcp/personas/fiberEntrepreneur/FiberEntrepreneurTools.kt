package com.app.backendplug_kmp.mcp.personas.fiberEntrepreneur

import com.app.backendplug_kmp.core.source.JsonTableSource
import com.app.backendplug_kmp.rag.clients.LlmClient
import com.app.backendplug_kmp.rag.personas.fiberEntrepreneur.CensusDemographicsClient
import com.app.backendplug_kmp.rag.personas.fiberEntrepreneur.MarketOpportunityPipeline
import com.app.backendplug_kmp.rag.personas.fiberEntrepreneur.toText
import io.ktor.client.HttpClient
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
    * Fiber Entrepreneur persona: Module 1 (Market Opportunity Intelligence) from
    * the Kumbaya PRD. Registers one tool, market_opportunity_report, backed by
    * MarketOpportunityPipeline (real FCC + Census data, deterministic scoring —
    * the LLM only narrates numbers already computed, it never generates them).
    * Works for any US county; Census demographics activate automatically when
    * CENSUS_API_KEY is set, and degrade gracefully (broadband-only scoring) when
    * it isn't.
*/
fun registerFiberEntrepreneurTools(
    server: Server,
    jsonSource: JsonTableSource,
    llm: LlmClient,
    httpClient: HttpClient,
    censusApiKey: String?
) {
    val censusClient = censusApiKey
        ?.takeIf { it.isNotBlank() }
        ?.let { CensusDemographicsClient(httpClient, it) }
    val pipeline = MarketOpportunityPipeline(jsonSource, llm, censusClient)

    server.addTool(
        name = "market_opportunity_report",
        description = "Fiber Entrepreneur: analyze the fiber broadband market opportunity for any US county. " +
                "Fetches FCC Form 477 broadband deployment records for the county, computes a deterministic " +
                "broadband gap analysis and a 0-100 Fiber Opportunity Score, then returns a grounded narrative " +
                "report. Also includes Census ACS demographic context (population, income, growth) when " +
                "CENSUS_API_KEY is configured. Arguments: location (string), stateFips (string, 2-digit FIPS), " +
                "countyFips (string, 3-digit FIPS), question (string, optional).",
        inputSchema = ToolSchema(
            properties = JsonObject(mapOf(
                "location" to JsonObject(mapOf(
                    "type" to JsonPrimitive("string"),
                    "description" to JsonPrimitive("Human-readable name of the county or city and state being analyzed")
                )),
                "stateFips" to JsonObject(mapOf(
                    "type" to JsonPrimitive("string"),
                    "description" to JsonPrimitive("2-digit state FIPS code (standard US Census FIPS numbering)")
                )),
                "countyFips" to JsonObject(mapOf(
                    "type" to JsonPrimitive("string"),
                    "description" to JsonPrimitive("3-digit county FIPS code (standard US Census FIPS numbering)")
                )),
                "question" to JsonObject(mapOf(
                    "type" to JsonPrimitive("string"),
                    "description" to JsonPrimitive("Optional specific angle for the narrative (defaults to a general investor summary)")
                ))
            )),
            required = listOf("location", "stateFips", "countyFips")
        )
    ) { request ->
        val location = request.arguments?.get("location")?.jsonPrimitive?.content
        val stateFips = request.arguments?.get("stateFips")?.jsonPrimitive?.content
        val countyFips = request.arguments?.get("countyFips")?.jsonPrimitive?.content
        val question = request.arguments?.get("question")?.jsonPrimitive?.content

        if (location.isNullOrBlank() || stateFips.isNullOrBlank() || countyFips.isNullOrBlank()) {
            CallToolResult(content = listOf(TextContent(text = "Provide 'location', 'stateFips', and 'countyFips' arguments.")))
        } else {
            val report = pipeline.run(location, stateFips, countyFips, question)
            CallToolResult(content = listOf(TextContent(text = report.toText())))
        }
    }
}

