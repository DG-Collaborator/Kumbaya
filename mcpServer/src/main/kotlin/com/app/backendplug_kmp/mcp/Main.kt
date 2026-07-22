package com.app.backendplug_kmp.mcp

import com.app.backendplug_kmp.core.domain.DataTable
import com.app.backendplug_kmp.core.net.createHttpClient
import com.app.backendplug_kmp.core.source.JsonTableSource
import com.app.backendplug_kmp.core.source.SourceQuery
import com.app.backendplug_kmp.core.source.SqlDataSource
import com.app.backendplug_kmp.mcp.personas.fiberConstructionManager.registerFiberConstructionManagerTools
import com.app.backendplug_kmp.mcp.personas.fiberEntrepreneur.registerFiberEntrepreneurTools
import com.app.backendplug_kmp.rag.RagPipeline
import com.app.backendplug_kmp.rag.SourceResolver
import com.app.backendplug_kmp.rag.clients.LlmClient
import com.app.backendplug_kmp.rag.clients.OpenAiClient
import com.app.backendplug_kmp.rag.sourceResolvers.ArcGisCatalogResolver
import com.app.backendplug_kmp.rag.sourceResolvers.CatalogSourceResolver
import com.app.backendplug_kmp.rag.sourceResolvers.DataGovCatalogResolver
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.jsonPrimitive

/**
 * Composition root: builds the shared clients/resolvers once, then hands
 * them to each persona's tool registration function. General-purpose
 * data-access tools (fetch_json, query_sql, ask, search_datasets) live here
 * directly since they're not persona-specific; persona-scoped tools live
 * under mcp/personas/<persona>/.
 *
 * Registers eight tools total:
 *   fetch_json / query_sql / ask / search_datasets           - general-purpose
 *   find_dataset / discover_and_ask / fiber_pre_construction - Fiber Construction Manager
 *   market_opportunity_report                                - Fiber Entrepreneur
 *
 * Transport is stdio, which means the protocol owns stdout: nothing here may
 * println to stdout; any diagnostics must go to stderr only.
 */
fun main() = runBlocking {
    val server = Server(
        serverInfo = Implementation(
            name = "backendplug",
            version = "0.1.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
    )

    // one HTTP client shared by the JSON source, the LLM client, and every catalog resolver
    val httpClient = createHttpClient()
    val jsonSource = JsonTableSource(httpClient)
    val sqlSource = SqlDataSource()

    val openAiKey = System.getenv("OPENAI_API_KEY")
        ?: error("OPENAI_API_KEY environment variable is not set, export it before starting the server.")
    val llm: LlmClient = OpenAiClient(httpClient, apiKey = openAiKey)

    val socrataResolver: SourceResolver = CatalogSourceResolver(httpClient, resultLimit = 30)
    val arcgisResolver: SourceResolver = ArcGisCatalogResolver(httpClient, resultLimit = 30)
    val dataGovResolver: SourceResolver = DataGovCatalogResolver(httpClient, resultLimit = 30)

    // optional: enables Census demographic context in market_opportunity_report; degrades gracefully if unset
    val censusApiKey = System.getenv("CENSUS_API_KEY")

    // tool 1: fetch any JSON URL and render it as a table
    server.addTool(
        name = "fetch_json",
        description = "Fetch a URL returning JSON and render it as a table. Argument: url (string).",
        inputSchema = ToolSchema(
            properties = JsonObject(mapOf(
                "url" to JsonObject(mapOf(
                    "type" to JsonPrimitive("string"),
                    "description" to JsonPrimitive("JSON URL to fetch and render as a table")
                ))
            )),
            required = listOf("url")
        )
    ) { request ->
        val url = request.arguments?.get("url")?.jsonPrimitive?.content
        if (url.isNullOrBlank()) {
            CallToolResult(content = listOf(TextContent(text = "Provide a 'url' argument.")))
        } else {
            val table = jsonSource.fetch(SourceQuery(address = url))
            CallToolResult(content = listOf(TextContent(text = renderTable(table))))
        }
    }

    // tool 2: run a SQL SELECT and render the result set as a table
    server.addTool(
        name = "query_sql",
        description = "Run a SQL SELECT and render the result as a table. Arguments: jdbcUrl (string), statement (string).",
        inputSchema = ToolSchema(
            properties = JsonObject(mapOf(
                "jdbcUrl" to JsonObject(mapOf(
                    "type" to JsonPrimitive("string"),
                    "description" to JsonPrimitive("JDBC connection URL (e.g. jdbc:sqlite:/path/to/db)")
                )),
                "statement" to JsonObject(mapOf(
                    "type" to JsonPrimitive("string"),
                    "description" to JsonPrimitive("SQL SELECT statement to execute")
                ))
            )),
            required = listOf("jdbcUrl", "statement")
        )
    ) { request ->
        val jdbcUrl = request.arguments?.get("jdbcUrl")?.jsonPrimitive?.content
        val statement = request.arguments?.get("statement")?.jsonPrimitive?.content
        if (jdbcUrl.isNullOrBlank() || statement.isNullOrBlank()) {
            CallToolResult(content = listOf(TextContent(text = "Provide 'jdbcUrl' and 'statement' arguments.")))
        } else {
            val table = sqlSource.fetch(SourceQuery(address = jdbcUrl, statement = statement))
            CallToolResult(content = listOf(TextContent(text = renderTable(table))))
        }
    }

    /*
       tool 3: fetch a JSON source, index its rows, and answer a question
               grounded in that data, the full seam -> RAG loop in one call
    */
    server.addTool(
        name = "ask",
        description = "Fetch a JSON URL, index its rows, and answer a question grounded in that data. Arguments: url (string), question (string).",
        inputSchema = ToolSchema(
            properties = JsonObject(mapOf(
                "url" to JsonObject(mapOf(
                    "type" to JsonPrimitive("string"),
                    "description" to JsonPrimitive("JSON URL to fetch and index for answering")
                )),
                "question" to JsonObject(mapOf(
                    "type" to JsonPrimitive("string"),
                    "description" to JsonPrimitive("Question to answer from the fetched data")
                ))
            )),
            required = listOf("url", "question")
        )
    ) { request ->
        val url = request.arguments?.get("url")?.jsonPrimitive?.content
        val question = request.arguments?.get("question")?.jsonPrimitive?.content
        if (url.isNullOrBlank() || question.isNullOrBlank()) {
            CallToolResult(content = listOf(TextContent(text = "Provide 'url' and 'question' arguments.")))
        } else {
            // fresh pipeline per call so the index holds only this source's rows
            val pipeline = RagPipeline(llm)
            pipeline.ingest(jsonSource.fetch(SourceQuery(address = url)))
            CallToolResult(content = listOf(TextContent(text = pipeline.answer(question))))
        }
    }

    // tool 4: parallel search across catalogs, grouped by source
    server.addTool(
        name = "search_datasets",
        description = "Search open-data catalogs for datasets matching a topic. Returns results grouped by source. Argument: query (string).",
        inputSchema = ToolSchema(
            properties = JsonObject(mapOf(
                "query" to JsonObject(mapOf(
                    "type" to JsonPrimitive("string"),
                    "description" to JsonPrimitive("Topic to search for")
                ))
            )),
            required = listOf("query")
        )
    ) { request ->
        val query = request.arguments?.get("query")?.jsonPrimitive?.content
        if (query.isNullOrBlank()) {
            CallToolResult(content = listOf(TextContent(text = "Provide a 'query' argument.")))
        } else {
            val (socrata, arcgis, datagov) = coroutineScope {
                val s = async { runCatching { socrataResolver.search(query) }.getOrDefault(emptyList()) }
                val a = async { runCatching { arcgisResolver.search(query) }.getOrDefault(emptyList()) }
                val d = async { runCatching { dataGovResolver.search(query) }.getOrDefault(emptyList()) }
                Triple(s.await(), a.await(), d.await())
            }
            val sections = listOf(
                "Socrata"    to socrata,
                "ArcGIS Hub" to arcgis,
                "data.gov"   to datagov
            ).filter { (_, results) -> results.isNotEmpty() }

            if (sections.isEmpty()) {
                CallToolResult(content = listOf(TextContent(text = "No datasets found for: $query")))
            } else {
                val text = sections.joinToString("\n\n---\n\n") { (source, candidates) ->
                    "[$source]\n" + candidates.joinToString("\n\n") { c ->
                        "${c.name}\n${c.description}\n${c.url}"
                    }
                }
                CallToolResult(content = listOf(TextContent(text = text)))
            }
        }
    }

    // persona-scoped tools
    registerFiberConstructionManagerTools(
        server = server,
        llm = llm,
        jsonSource = jsonSource,
        socrataResolver = socrataResolver,
        arcgisResolver = arcgisResolver,
        dataGovResolver = dataGovResolver
    )
    registerFiberEntrepreneurTools(
        server = server,
        jsonSource = jsonSource,
        llm = llm,
        httpClient = httpClient,
        censusApiKey = censusApiKey
    )

    // stdio transport: kotlinx-io Source/Sink wrapped over the process streams
    val transport = StdioServerTransport(
        input = System.`in`.asSource().buffered(),
        output = System.out.asSink().buffered()
    )

    // start the session, then stay alive until the client disconnects
    server.createSession(transport)
    val done = Job()
    server.onClose { done.complete() }
    done.join()
}

/**
    * Flattens a DataTable to readable text for a tool result, capped so a large
    * result set can't blow up the response.
*/
private fun renderTable(table: DataTable): String {
    if (table.isEmpty) return "No tabular data found."
    val keys = table.columns.map { it.key }
    return buildString {
        appendLine("${table.rows.size} rows × ${keys.size} columns")
        appendLine(keys.joinToString(" | "))
        append(table.rows.take(100).joinToString("\n") { row ->
            keys.joinToString(" | ") { row.value(it) }
        })
    }
}