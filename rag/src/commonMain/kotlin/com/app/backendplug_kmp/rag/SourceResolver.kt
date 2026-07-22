package com.app.backendplug_kmp.rag

import com.app.backendplug_kmp.rag.clients.LlmClient

/**
    * A dataset the resolver can hand back: a human description plus the address
    * (URL) BackendPlug's JSON source can fetch.
*/
data class SourceCandidate(
    val name: String,
    val description: String,
    val url: String
)

/**
    * Turns a free-text description ("wildlife", "recent us highway data") into
    * candidate sources, best match first, each with an address the JSON source
    * can fetch.

    * One seam, swappable implementations: a curated registry or a live
    * open-data catalog search, with no change to callers.
*/
interface SourceResolver {
    suspend fun search(description: String): List<SourceCandidate>

    /**
        * Convenience for callers that only want the single best match
        * (like the MCP find_dataset / discover_and_ask tools).
    */
    suspend fun resolve(description: String): SourceCandidate? =
        search(description).firstOrNull()
}

/**
    * Resolves against a curated registry by semantic similarity: embeds each
    * candidate's description once, then returns the candidate whose description
    * is closest to the query. Reuses the very same embeddings + vector index as
    * the RAG pipeline.
*/
class RegistrySourceResolver(
    private val llm: LlmClient,
    private val registry: List<SourceCandidate>
) : SourceResolver {

    private val index = InMemoryVectorIndex()
    private val byDescription = registry.associateBy { it.description }
    private var indexed = false

    // embed the registry lazily on first use, so construction stays cheap
    private suspend fun ensureIndexed() {
        if (indexed) return
        for (candidate in registry) {
            index.add(candidate.description, llm.embed(candidate.description))
        }
        indexed = true
    }

    override suspend fun search(description: String): List<SourceCandidate> {
        ensureIndexed()
        val queryVector = llm.embed(description)
        // the index already ranks by cosine similarity; map the hits back to candidates
        return index.search(queryVector, topK = 5).mapNotNull { byDescription[it] }
    }
}

