package com.app.backendplug_kmp.rag.sourceResolvers

import com.app.backendplug_kmp.rag.SourceCandidate
import com.app.backendplug_kmp.rag.SourceResolver
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
    * Fans a query out to multiple catalog resolvers in parallel and returns their
    * results combined in label order. Each candidate's name is prefixed with its
    * source label (like "[Socrata] Traffic Accidents") so the UI shows provenance
    * without any changes to AskState or AskScreen.

    * A failure in one resolver is swallowed so the others still return results.
*/
class CompositeCatalogResolver(
    private val resolvers: List<Pair<String, SourceResolver>>
) : SourceResolver {

    override suspend fun search(description: String): List<SourceCandidate> =
        coroutineScope {
            resolvers
                .map { (label, resolver) ->
                    async {
                        runCatching { resolver.search(description) }
                            .getOrDefault(emptyList())
                            .map { candidate -> candidate.copy(name = "[$label] ${candidate.name}") }
                    }
                }
                .flatMap { it.await() }
        }
}