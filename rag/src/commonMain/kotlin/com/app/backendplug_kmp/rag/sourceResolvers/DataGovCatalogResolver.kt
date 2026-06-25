package com.app.backendplug_kmp.rag.sourceResolvers

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import com.app.backendplug_kmp.rag.SourceCandidate
import com.app.backendplug_kmp.rag.SourceResolver

/**
    * Searches the data.gov CKAN catalog for datasets matching a free-text query.
    * One candidate per package, the first resource whose format is JSON, so the
    * same dataset doesn't appear twice when it has multiple JSON resources.

    * Same SourceResolver seam as CatalogSourceResolver; swap or combine at Main.
*/
class DataGovCatalogResolver(
    private val client: HttpClient,
    private val resultLimit: Int = 25
) : SourceResolver {

    override suspend fun search(description: String): List<SourceCandidate> {
        val response: CkanResponse = client.get("https://catalog.data.gov/api/3/action/package_search") {
            parameter("q", description)
            parameter("rows", resultLimit)
        }.body()

        return response.result.results.mapNotNull { pkg ->
            val resource = pkg.resources.firstOrNull {
                it.format.equals("JSON", ignoreCase = true)
            } ?: return@mapNotNull null
            SourceCandidate(
                name = pkg.title,
                description = pkg.notes?.replace(Regex("<[^>]+>"), "")?.take(200) ?: pkg.title,
                url = resource.url
            )
        }
    }
}

@Serializable
private data class CkanResponse(val result: CkanResult)

@Serializable
private data class CkanResult(val results: List<CkanPackage>)

@Serializable
private data class CkanPackage(
    val title: String,
    val notes: String? = null,
    val resources: List<CkanResource> = emptyList()
)

@Serializable
private data class CkanResource(
    val url: String,
    val format: String = ""
)
