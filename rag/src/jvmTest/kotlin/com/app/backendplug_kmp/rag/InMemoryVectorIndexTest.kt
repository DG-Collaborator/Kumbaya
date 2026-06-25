package com.app.backendplug_kmp.rag

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
    * Hermetic: uses hand-made vectors (no Ollama) so the expected ranking is
    * known exactly. Proves nearest-by-cosine retrieval before we wire in real
    * embeddings.
*/
class InMemoryVectorIndexTest {

    @Test
    fun retrievesMostSimilarFirst() {
        val index = InMemoryVectorIndex()
        index.add("cat", listOf(1f, 0f, 0f))
        index.add("dog", listOf(0.9f, 0.1f, 0f))   // close to cat
        index.add("car", listOf(0f, 0f, 1f))       // orthogonal to cat

        val results = index.search(listOf(1f, 0f, 0f), topK = 2)

        assertEquals(2, results.size)
        assertEquals("cat", results.first(), "identical vector should rank first")
        assertEquals("dog", results[1], "near vector should rank second")
        assertTrue("car" !in results, "orthogonal vector should fall outside top-2")
    }
}
