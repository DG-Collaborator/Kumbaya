package com.app.backendplug_kmp.rag.sourceResolvers

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import com.app.backendplug_kmp.rag.SourceCandidate
import com.app.backendplug_kmp.rag.SourceResolver

/**
    * Searches the ArcGIS Hub open-data catalog for datasets matching a free-text
    * query. Only results whose URL points to a Feature Service or Map Service layer
    * are kept, those are the endpoints JsonTableSource can query as JSON.

    * Same SourceResolver seam as CatalogSourceResolver; swap or combine at Main.
*/
class ArcGisCatalogResolver(
    private val client: HttpClient,
    private val resultLimit: Int = 25,
    private val rowLimit: Int = 50
) : SourceResolver {

    override suspend fun search(description: String): List<SourceCandidate> {
        val response: HubResponse = client.get("https://hub.arcgis.com/api/v3/datasets") {
            parameter("q", description)
            parameter("fields[datasets]", "name,description,url,type")
            parameter("page[size]", resultLimit)
        }.body()

        return response.data.mapNotNull { item ->
            val serviceUrl = item.attributes.url ?: return@mapNotNull null
            /*
               build the queryable JSON endpoint based on whether the URL already
               includes a layer index (like /FeatureServer/0) or just the service root
            */
            val url = when {
                serviceUrl.matches(Regex(".*/(FeatureServer|MapServer)/\\d+$")) ->
                    "$serviceUrl/query?where=1%3D1&outFields=*&returnGeometry=false&f=json"
                serviceUrl.matches(Regex(".*/(FeatureServer|MapServer)$")) ->
                    "$serviceUrl/0/query?where=1%3D1&outFields=*&returnGeometry=false&f=json"
                else -> return@mapNotNull null
            }
            SourceCandidate(
                name = item.attributes.name,
                description = item.attributes.description ?: item.attributes.name,
                url = url
            )
        }
    }
}

@Serializable
private data class HubResponse(val data: List<HubItem>)

@Serializable
private data class HubItem(val attributes: HubAttributes)

@Serializable
private data class HubAttributes(
    val name: String,
    val description: String? = null,
    val url: String? = null,
    val type: String? = null
)
