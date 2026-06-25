package com.app.backendplug_kmp.rag.clients

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
    * Searches the web via Tavily and returns results as a single context string
    * ready to pass directly to LlmClient.generate(). No embedding needed -
    * the snippets are short enough to fit in one prompt.

    * Swap this for a DataLakeClient at the same call site in AskViewModel when
    * a data lake is ready; nothing else changes.
*/
class WebSearchClient(
    private val client: HttpClient,
    private val apiKey: String,
    private val maxResults: Int = 10
) {
    suspend fun search(query: String): String {
        val httpResponse = client.post("https://api.tavily.com/search") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(TavilyRequest(query = query, maxResults = maxResults))
        }
        if (!httpResponse.status.isSuccess()) {
            error("Tavily search failed (${httpResponse.status.value}) — check that TAVILY_API_KEY is set correctly.")
        }
        val response: TavilyResponse = httpResponse.body()
        if (response.results.isEmpty()) return "No results found for: $query"
        return response.results.joinToString("\n\n") { "${it.title}\n${it.content}" }
    }
}

@Serializable
private data class TavilyRequest(
    val query: String,
    @SerialName("search_depth") val searchDepth: String = "basic",
    @SerialName("max_results") val maxResults: Int = 10
)

@Serializable
private data class TavilyResponse(val results: List<TavilyResult> = emptyList())

@Serializable
private data class TavilyResult(
    val title: String,
    val content: String,
    val url: String
)