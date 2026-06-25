package com.app.backendplug_kmp.rag

import kotlin.math.sqrt

/**
    * A tiny in-memory vector store: holds texts alongside their embedding
    * vectors, and retrieves the texts most similar to a query vector by cosine
    * similarity.

    * "In-memory first" by design, it proves the RAG retrieval loop with zero
    * dependencies. A persistent store (SQLite + sqlite-vec) can replace it
    * later behind the same add/search shape.
*/
class InMemoryVectorIndex {

    // one stored document: its text and the vector that represents it
    private data class Entry(val text: String, val vector: List<Float>)

    private val entries = mutableListOf<Entry>()

    val size: Int get() = entries.size

    // index a piece of text under its precomputed embedding
    fun add(text: String, vector: List<Float>) {
        entries += Entry(text, vector)
    }

    /**
        * Returns the topK texts whose vectors are most similar to the query
        * vector, most-similar first. With fewer than topK entries it returns
        * everything it has, ranked.
    */
    fun search(queryVector: List<Float>, topK: Int = 4): List<String> =
        entries
            .map { entry -> entry.text to cosineSimilarity(queryVector, entry.vector) }
            .sortedByDescending { (_, score) -> score }
            .take(topK)
            .map { (text, _) -> text }
}

/**
    * Cosine similarity = how aligned two vectors are, ignoring length. 1.0 means
    * identical direction, 0.0 means unrelated (orthogonal). It's the standard
    * relevance measure for text embeddings.
*/
private fun cosineSimilarity(a: List<Float>, b: List<Float>): Double {
    require(a.size == b.size) { "vector dimensions differ: ${a.size} vs ${b.size}" }

    var dot = 0.0
    var normA = 0.0
    var normB = 0.0
    for (i in a.indices) {
        dot += a[i] * b[i]
        normA += a[i] * a[i]
        normB += b[i] * b[i]
    }

    val denominator = sqrt(normA) * sqrt(normB)
    // guard against a zero-length vector (no direction to compare)
    return if (denominator == 0.0) 0.0 else dot / denominator
}