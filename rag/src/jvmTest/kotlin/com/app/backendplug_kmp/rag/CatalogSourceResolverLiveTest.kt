package com.app.backendplug_kmp.rag

import com.app.backendplug_kmp.core.net.createHttpClient
import com.app.backendplug_kmp.rag.sourceResolvers.CatalogSourceResolver
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Live (network, no Ollama): a free-text category resolves to a real Socrata
 * dataset with a fetchable JSON data URL.
 */
class CatalogSourceResolverLiveTest {

    @Test
    fun resolvesCategoryToSocrataDataset() = runBlocking {
        val resolver = CatalogSourceResolver(createHttpClient())

        val match = resolver.resolve("traffic accidents")

        assertNotNull(match, "expected a catalog match")
        assertTrue(
            match.url.contains("/resource/") && match.url.contains(".json"),
            "expected a Socrata .json data url, got: ${match.url}"
        )
    }
}