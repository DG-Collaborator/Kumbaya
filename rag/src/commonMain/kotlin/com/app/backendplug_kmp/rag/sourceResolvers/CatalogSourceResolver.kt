package com.app.backendplug_kmp.rag.sourceResolvers

import com.app.backendplug_kmp.rag.SourceCandidate
import com.app.backendplug_kmp.rag.SourceResolver
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

/**
 * Open-ended discovery: searches the Socrata open-data catalog for a free-text
 * query. If the query mentions a known city or state, the search is scoped to
 * that city's Socrata portal automatically. Otherwise the global catalog is
 * searched and results may come from any city.
 */
class CatalogSourceResolver(
    private val client: HttpClient,
    private val resultLimit: Int = 25,
    private val rowLimit: Int = 50
) : SourceResolver {

    override suspend fun search(description: String): List<SourceCandidate> {
        val domain = detectDomain(description)
        val response: CatalogResponse = client.get("https://api.us.socrata.com/api/catalog/v1") {
            parameter("q", description)
            parameter("only", "dataset")
            parameter("limit", resultLimit)
            if (domain != null) parameter("domains", domain)
        }.body()

        return response.results.map { result ->
            val url =
                "https://${result.metadata.domain}/resource/${result.resource.id}.json?\$limit=$rowLimit"
            SourceCandidate(
                name = result.resource.name,
                description = result.resource.description ?: result.resource.name,
                url = url
            )
        }
    }

    private fun detectDomain(query: String): String? {
        val q = query.lowercase()
        return when {
            "new york" in q || "nyc" in q || "manhattan" in q ||
            "brooklyn" in q || "bronx" in q || "queens" in q   -> "data.cityofnewyork.us"
            "san diego" in q                                   -> "internal-sandiegocounty.data.socrata.com"
            "los angeles" in q || "lacity" in q                -> "data.lacity.org"
            "san francisco" in q || "sfgov" in q               -> "data.sfgov.org"
            "chicago" in q                                     -> "data.cityofchicago.org"
            "seattle" in q                                     -> "data.seattle.gov"
            "austin" in q                                      -> "data.austintexas.gov"
            "boston" in q                                      -> "data.boston.gov"
            "denver" in q                                      -> "data.denvergov.org"
            "philadelphia" in q || "philly" in q               -> "data.phila.gov"
            else                                               -> null
        }
    }
}

@Serializable
private data class CatalogResponse(val results: List<CatalogResult>)

@Serializable
private data class CatalogResult(val resource: CatalogResource, val metadata: CatalogMetadata)

@Serializable
private data class CatalogResource(
    val id: String,
    val name: String,
    val description: String? = null
)

@Serializable
private data class CatalogMetadata(val domain: String)