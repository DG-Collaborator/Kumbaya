package com.app.backendplug_kmp.mcp.personas.fiberConstructionManager;

import com.app.backendplug_kmp.core.source.JsonTableSource
import com.app.backendplug_kmp.core.source.SourceQuery
import com.app.backendplug_kmp.rag.RagPipeline
import com.app.backendplug_kmp.rag.SourceResolver
import com.app.backendplug_kmp.rag.clients.LlmClient
import com.app.backendplug_kmp.rag.personas.fiberConstructionManager.fiberConstructionSearchTerms
import com.app.backendplug_kmp.rag.sourceResolvers.CompositeCatalogResolver
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
    * Fiber Construction Manager persona: pre-construction intelligence for an
    * already-decided fiber build in any US city (GIS, permitting, environmental,
    * pole attachment, etc.) — distinct from the Fiber Entrepreneur persona, which
    * evaluates whether a build should happen at all.

    * All three tools run on the same live, nationwide catalog resolvers
    * (Socrata + ArcGIS Hub + data.gov) rather than a curated regional registry —
    * a static list can't honestly cover every US city without fabricating
    * unverified dataset entries.

    * Registers three tools: find_dataset, discover_and_ask, fiber_pre_construction.
*/
fun registerFiberConstructionManagerTools(
    server: Server,
    llm: LlmClient,
    jsonSource: JsonTableSource,
    socrataResolver: SourceResolver,
    arcgisResolver: SourceResolver,
    dataGovResolver: SourceResolver
) {
    val resolver = CompositeCatalogResolver(listOf(
        "Socrata"    to socrataResolver,
        "ArcGIS Hub" to arcgisResolver,
        "data.gov"   to dataGovResolver
    ))

    // tool: resolve a free-text description to a live dataset (name + url)
    server.addTool(
        name = "find_dataset",
        description = "Find a dataset from a natural-language description via live catalog search (Socrata, ArcGIS Hub, data.gov). Argument: description (string).",
        inputSchema = ToolSchema(
            properties = JsonObject(mapOf(
                "description" to JsonObject(mapOf(
                    "type" to JsonPrimitive("string"),
                    "description" to JsonPrimitive("Natural-language description of the dataset to find")
                ))
            )),
            required = listOf("description")
        )
    ) { request ->
        val description = request.arguments?.get("description")?.jsonPrimitive?.content
        if (description.isNullOrBlank()) {
            CallToolResult(content = listOf(TextContent(text = "Provide a 'description' argument.")))
        } else {
            val match = resolver.resolve(description)
            val text = if (match == null) "No matching dataset found."
            else "${match.name}\n${match.url}"
            CallToolResult(content = listOf(TextContent(text = text)))
        }
    }

    // tool: the full experience, find a dataset live, fetch it, answer about it
    server.addTool(
        name = "discover_and_ask",
        description = "Find a dataset from a description via live catalog search, fetch it, and answer a question grounded in it. Arguments: description (string), question (string).",
        inputSchema = ToolSchema(
            properties = JsonObject(mapOf(
                "description" to JsonObject(mapOf(
                    "type" to JsonPrimitive("string"),
                    "description" to JsonPrimitive("Natural-language description of the dataset to find")
                )),
                "question" to JsonObject(mapOf(
                    "type" to JsonPrimitive("string"),
                    "description" to JsonPrimitive("Question to answer from the discovered dataset")
                ))
            )),
            required = listOf("description", "question")
        )
    ) { request ->
        val description = request.arguments?.get("description")?.jsonPrimitive?.content
        val question = request.arguments?.get("question")?.jsonPrimitive?.content
        if (description.isNullOrBlank() || question.isNullOrBlank()) {
            CallToolResult(content = listOf(TextContent(text = "Provide 'description' and 'question' arguments.")))
        } else {
            val match = resolver.resolve(description)
            if (match == null) {
                CallToolResult(content = listOf(TextContent(text = "No matching dataset found for: $description")))
            } else {
                val pipeline = RagPipeline(llm)
                pipeline.ingest(jsonSource.fetch(SourceQuery(address = match.url)))
                CallToolResult(content = listOf(TextContent(text = "Source: ${match.name}\n\n${pipeline.answer(question)}")))
            }
        }
    }

    // tool: fiber construction pre-construction intelligence agent, any US city
    server.addTool(
        name = "fiber_pre_construction",
        description = """
                Fiber construction pre-construction intelligence agent for any US city or metro area.
                Given a construction phase and a location, searches GIS, permitting, broadband, and
                environmental public data catalogs and returns a grounded answer with source attribution.
                Phases: gis_mapping | utility_infrastructure | existing_fiber | broadband_demand |
                        route_engineering | permitting | pole_attachment | environmental |
                        construction_planning | efit
                Arguments: location (string), phase (string), question (string).
            """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(mapOf(
                "location" to JsonObject(mapOf(
                    "type"        to JsonPrimitive("string"),
                    "description" to JsonPrimitive("City, neighborhood, zip code, or county anywhere in the US")
                )),
                "phase" to JsonObject(mapOf(
                    "type"        to JsonPrimitive("string"),
                    "description" to JsonPrimitive("Construction phase: gis_mapping | utility_infrastructure | existing_fiber | broadband_demand | route_engineering | permitting | pole_attachment | environmental | construction_planning | efit")
                )),
                    "question" to JsonObject(mapOf(
                        "type"        to JsonPrimitive("string"),
                        "description" to JsonPrimitive("Specific pre-construction question to answer grounded in public data")
                    ))
                )),
                required = listOf("location", "phase", "question")
            )
        ) { request ->

            val location = request.arguments?.get("location")?.jsonPrimitive?.content
            val phase    = request.arguments?.get("phase")?.jsonPrimitive?.content
            val question = request.arguments?.get("question")?.jsonPrimitive?.content

            if (location.isNullOrBlank() || phase.isNullOrBlank() || question.isNullOrBlank()) {
                CallToolResult(content = listOf(TextContent(text = "Provide 'location', 'phase', and 'question' arguments.")))
            } else {
                val searchTerms = fiberConstructionSearchTerms(phase, location)
                System.err.println("[fiber_pre_construction] phase=$phase location=$location query='$searchTerms'")

                // fan out across all data sources in parallel
                val (socrata, arcgis, datagov) = coroutineScope {
                    val s = async { runCatching { socrataResolver.search(searchTerms) }.getOrDefault(emptyList()) }
                    val a = async { runCatching { arcgisResolver.search(searchTerms) }.getOrDefault(emptyList()) }
                    val d = async { runCatching { dataGovResolver.search(searchTerms) }.getOrDefault(emptyList()) }
                    Triple(s.await(), a.await(), d.await())
                }

                val candidates = (socrata + arcgis + datagov).distinctBy { it.url }.take(4)

                if (candidates.isEmpty()) {
                    CallToolResult(content = listOf(TextContent(text =
                        "No datasets found for phase '$phase' in '$location'.\n" +
                                "Try search_datasets with a broader query, e.g. '$location $phase fiber'."
                    )))
                } else {
                    val pipeline = RagPipeline(llm)
                    val ingested = mutableListOf<String>()
                    for (candidate in candidates) {
                        try {
                            pipeline.ingest(jsonSource.fetch(SourceQuery(address = candidate.url)))
                            ingested += candidate.name
                        } catch (e: Exception) {
                            System.err.println("[fiber_pre_construction] skipped '${candidate.name}': ${e.message}")
                        }
                    }

                    if (ingested.isEmpty()) {
                        CallToolResult(content = listOf(TextContent(text =
                            "Datasets were found but could not be fetched. Verify the dataset is still current or check network access."
                        )))
                    } else {
                        val sourceList = ingested.joinToString("\n") { "  • $it" }
                        val answer = pipeline.answer("For $location — $question")
                        CallToolResult(content = listOf(TextContent(text =
                            "Phase: $phase | Location: $location\n" +
                                    "Sources consulted:\n$sourceList\n\n" +
                                    answer
                        )))
                    }
                }
            }
        }
}