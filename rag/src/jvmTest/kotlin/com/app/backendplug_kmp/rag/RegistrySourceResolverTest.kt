package com.app.backendplug_kmp.rag

import com.app.backendplug_kmp.core.net.createHttpClient
import com.app.backendplug_kmp.rag.clients.OllamaClient
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
    * Live (needs Ollama): proves a natural-language description resolves to the
    * right registered dataset by semantic similarity, not keyword matching.
*/
class RegistrySourceResolverTest {

    @Test
    fun resolvesHighwayDescriptionToFreewaySource() = runBlocking {
        val resolver = RegistrySourceResolver(
            OllamaClient(createHttpClient()),
            DefaultSourceRegistry.entries
        )

        val match = resolver.resolve("find me recent us highway data")

        assertNotNull(match, "expected a registry match")
        assertEquals("USA Freeway System", match.name)
    }
}
