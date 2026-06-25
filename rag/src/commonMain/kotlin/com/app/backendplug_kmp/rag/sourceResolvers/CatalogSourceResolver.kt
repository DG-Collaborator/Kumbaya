package com.app.backendplug_kmp.rag.sourceResolvers

import com.app.backendplug_kmp.rag.SourceCandidate
import com.app.backendplug_kmp.rag.SourceResolver
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

/**
    * Open-ended discovery: searches the Socrata open-data catalog (a platform
    * behind hundreds of government data portals) for a free-text category and
    * returns the top dataset as a fetchable JSON source.

    * Same SourceResolver seam as RegistrySourceResolver, swapping one for the
    * other is a one-line change at the composition root; nothing above changes.

    * rowLimit caps the data URL: each ingested row becomes one Ollama embed
    * call, so keep it modest for a responsive UI.
*/
class CatalogSourceResolver(
    private val client: HttpClient,
    private val resultLimit: Int = 25,
    private val rowLimit: Int = 50
) : SourceResolver {

    override suspend fun search(description: String): List<SourceCandidate> {
        val response: CatalogResponse = client.get("https://api.us.socrata.com/api/catalog/v1") {
            parameter("q", description)
            parameter("only", "dataset")
            parameter("limit", resultLimit)
        }.body()

        return response.results.map { result ->
            // every Socrata dataset is queryable as a JSON array at this endpoint
            val url =
                "https://${result.metadata.domain}/resource/${result.resource.id}.json?\$limit=$rowLimit"
            SourceCandidate(
                name = result.resource.name,
                description = result.resource.description ?: result.resource.name,
                url = url
            )
        }
    }
}

// only the fields we need; the client ignores the rest
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