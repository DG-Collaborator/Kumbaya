package com.app.backendplug_kmp.rag

import com.app.backendplug_kmp.core.net.createHttpClient
import com.app.backendplug_kmp.rag.clients.OllamaClient
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
    * Live (needs Ollama): proves a natural-language description resolves to the
    * right candidate by semantic similarity, not keyword matching. Uses an
    * inline fixture list rather than any persona's registry, since no current
    * persona wires RegistrySourceResolver into production — this test verifies
    * the resolver itself, independent of any specific dataset list.
*/
class RegistrySourceResolverTest {

    private val fixtures = listOf(
        SourceCandidate(
            name = "USA Freeway and Highway Network",
            description = "United States highways, freeways, and interstate routes for route planning",
            url = "https://example.org/highways.json"
        ),
        SourceCandidate(
            name = "National Wetlands Inventory",
            description = "Wetland and surface water habitat boundaries for environmental review",
            url = "https://example.org/wetlands.json"
        )
    )

    @Test
    fun resolvesHighwayDescriptionToFreewaySource() = runBlocking {
        val resolver = RegistrySourceResolver(OllamaClient(createHttpClient()), fixtures)

        val match = resolver.resolve("find me recent us highway data")

        assertNotNull(match, "expected a fixture match")
        assertEquals("USA Freeway and Highway Network", match.name)
    }
}

