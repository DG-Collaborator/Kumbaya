package com.app.backendplug_kmp.rag.sourceResolvers

import com.app.backendplug_kmp.rag.SourceCandidate
import com.app.backendplug_kmp.rag.SourceResolver
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
    * Queries the Accela Construct API for permit records relevant to a search query.

    * Composition root swap point: provide ACCELA_AGENCY_ID + ACCELA_APP_SECRET
    * environment variables (read in mcpServer/Main.kt) to activate live permit data.
    * Without credentials this resolver returns emptyList - fiber_pre_construction
    * falls back to public Socrata and data.gov permit datasets automatically.

    * When credentials are available:
    *   ACCELA_AGENCY_ID  - agency environment ID (like "SANDIEGOCA")
    *   ACCELA_APP_SECRET - app secret from Accela developer console

    * To activatecehk the TODO blocks, no other files change.
*/
class AccelaPermitResolver(
    private val client: HttpClient,
    private val agencyId: String? = null,
    private val appSecret: String? = null,
    private val environment: String = "PROD"
) : SourceResolver {

    private val isConfigured get() = !agencyId.isNullOrBlank() && !appSecret.isNullOrBlank()

    override suspend fun search(description: String): List<SourceCandidate> {
        if (!isConfigured) return emptyList()

        // TODO: fetch OAuth2 bearer token
        /*
             POST https://auth.accela.com/oauth2/token
             grant_type=client_credentials
             agency_name=$agencyId
             environment=$environment
             client_id=<your app id>
             client_secret=$appSecret
        */
        val bearerToken = fetchToken() ?: return emptyList()

        // TODO: search Accela Construct API for permit records
        /*
             GET https://apis.accela.com/v4/records
             Authorization: Bearer $bearerToken
             keyword=$description
             agency_name=$agencyId
             environment=$environment
        */
        val records = fetchRecords(bearerToken, description)

        return records.map { record ->
            SourceCandidate(
                name  = "Accela Permit — ${record.id}",
                description = "${record.type} permit: ${record.description}",
                url   = "https://apis.accela.com/v4/records/${record.id}?agency_name=$agencyId&environment=$environment"
            )
        }
    }

    private suspend fun fetchToken(): String? {
        // TODO: implement later
        return null
    }

    private suspend fun fetchRecords(token: String, query: String): List<AccelaRecord> {
        // TODO: implement later
        return emptyList()
    }
}

@Serializable
private data class AccelaRecord(
    val id: String,
    val type: String = "",
    val description: String = ""
)