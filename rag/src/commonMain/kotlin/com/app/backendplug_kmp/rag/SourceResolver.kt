package com.app.backendplug_kmp.rag

import com.app.backendplug_kmp.rag.clients.LlmClient

/**
    * A dataset the resolver can hand back: a human description plus the address
    * (URL) BackendPlug's JSON source can fetch.
*/
data class SourceCandidate(
    val name: String,
    val description: String,
    val url: String
)

/**
    * Turns a free-text description ("wildlife", "recent us highway data") into
    * candidate sources, best match first, each with an address the JSON source
    * can fetch.

    * One seam, swappable implementations: a curated registry or a live
    * open-data catalog search, with no change to callers.
*/
interface SourceResolver {
    suspend fun search(description: String): List<SourceCandidate>

    /**
        * Convenience for callers that only want the single best match
        * (like the MCP find_dataset / discover_and_ask tools).
    */
    suspend fun resolve(description: String): SourceCandidate? =
        search(description).firstOrNull()
}

/**
    * Resolves against a curated registry by semantic similarity: embeds each
    * candidate's description once, then returns the candidate whose description
    * is closest to the query. Reuses the very same embeddings + vector index as
    * the RAG pipeline.
*/
class RegistrySourceResolver(
    private val llm: LlmClient,
    private val registry: List<SourceCandidate>
) : SourceResolver {

    private val index = InMemoryVectorIndex()
    private val byDescription = registry.associateBy { it.description }
    private var indexed = false

    // embed the registry lazily on first use, so construction stays cheap
    private suspend fun ensureIndexed() {
        if (indexed) return
        for (candidate in registry) {
            index.add(candidate.description, llm.embed(candidate.description))
        }
        indexed = true
    }

    override suspend fun search(description: String): List<SourceCandidate> {
        ensureIndexed()
        val queryVector = llm.embed(description)
        // the index already ranks by cosine similarity; map the hits back to candidates
        return index.search(queryVector, topK = 5).mapNotNull { byDescription[it] }
    }
}

/**
 * San Diego fiber construction pre-construction knowledge base.
 * Descriptions are matched semantically by RegistrySourceResolver — phrase
 * them in the engineer's terms, not the dataset publisher's terms.
 *
 * URL format notes:
 *   ArcGIS REST (?f=json&returnGeometry=false) — parsed via features/attributes unwrap
 *   Socrata     (?$limit=N)                    — parsed as bare JSON array
 *
 * Socrata dataset IDs (4x4 codes): verify current IDs at data.sandiego.gov
 * before the demo by searching the dataset name in the portal.
 */
object DefaultSourceRegistry {
    val entries: List<SourceCandidate> = listOf(

        // GIS & Base Mapping
        SourceCandidate(
            name = "USA Freeway and Highway Network",
            description = "United States highways, freeways, and interstate routes for highway crossing identification and route planning in fiber construction",
            url  = "https://services.arcgis.com/P3ePLMYs2RVChkJx/arcgis/rest/services/USA_Freeway_System/FeatureServer/1/query?where=CLASS%3D%27I%27&outFields=*&returnGeometry=false&f=json"
    ),
    SourceCandidate(
    name = "FEMA National Flood Hazard Layer — San Diego County",
    description = "FEMA flood zone and special flood hazard area designations for San Diego County. Required environmental constraint for fiber route engineering and permit applications in flood-prone corridors",
    url  = "https://hazards.fema.gov/arcgis/rest/services/public/NFHL/MapServer/28/query?where=DFIRM_ID+LIKE+%2706073%25%27&outFields=DFIRM_ID,FLD_ZONE,ZONE_SUBTY,SFHA_TF&returnGeometry=false&resultRecordCount=200&f=json"
    ),

    // Permitting & Regulatory
    // Verify 4x4 IDs at data.sandiego.gov → search dataset name → copy resource ID
    SourceCandidate(
    name = "San Diego Right-of-Way Use Permits",
    description = "City of San Diego active encroachment, street opening, and right-of-way use permits. Primary source for permit status, active construction zones, and jurisdiction lookup for fiber construction permitting",
    url  = "https://data.sandiego.gov/resource/4dp3-jrh2.json?\$limit=200"
    ),
    SourceCandidate(
    name = "San Diego Traffic Control and Lane Closure Permits",
    description = "City of San Diego traffic control plans, lane closure permits, and active work zones. Required for fiber construction in city rights-of-way; shows conflicts near planned routes",
    url  = "https://data.sandiego.gov/resource/n4ze-k5bd.json?\$limit=200"
    ),
    SourceCandidate(
    name = "San Diego Development Services Permits",
    description = "City of San Diego Development Services Department building, grading, and construction permits including encroachment authorizations and project approval status",
    url  = "https://data.sandiego.gov/resource/yfvg-kfhh.json?\$limit=200"
    ),

    // Broadband Infrastructure & Demand
    SourceCandidate(
    name = "FCC Broadband Deployment Data",
    description = "FCC broadband provider deployment data showing existing fiber, cable, and fixed wireless coverage by location. Use for existing fiber network gap analysis and middle-mile identification in San Diego",
    url  = "https://opendata.fcc.gov/resource/whue-6pnt.json?\$limit=200"
    ),
    SourceCandidate(
    name = "California Middle Mile Broadband Initiative",
    description = "California state middle mile open-access fiber routes and broadband infrastructure assets. Use for interconnection planning and identifying existing state fiber in San Diego region",
    url  = "https://data.ca.gov/api/3/action/datastore_search?resource_id=9c6d3285-a7d0-4f11-a099-b3d8d6b4cd50&limit=200"
    ),

    // Environmental Constraints
    SourceCandidate(
    name = "San Diego County Sensitive Biological Resources",
    description = "San Diego County sensitive lands, biological resources, and habitat conservation areas. Required for California Coastal Commission and CDFW environmental review for fiber routes near sensitive areas",
    url  = "https://data.sandiego.gov/resource/sensitive-lands.json?\$limit=200"
    ),

    // Route Engineering & Infrastructure
    SourceCandidate(
    name = "SANDAG Regional Transportation Network",
    description = "SANDAG San Diego regional transportation corridors, roads, transit lines, and infrastructure for multi-jurisdiction fiber route planning and crossing identification",
    url  = "https://opendata.arcgis.com/api/v3/datasets/sandag-regional-transportation/query?where=1%3D1&outFields=*&returnGeometry=false&resultRecordCount=200&f=json"
    ),
    SourceCandidate(
    name = "Caltrans District 11 Highway Performance Data",
    description = "Caltrans District 11 (San Diego/Imperial) highway performance monitoring data for state route crossings, traffic volumes, and Caltrans encroachment permit jurisdiction identification",
    url  = "https://data.ca.gov/api/3/action/datastore_search?resource_id=caltrans-hpms-d11&limit=200"
    ),
    )
}