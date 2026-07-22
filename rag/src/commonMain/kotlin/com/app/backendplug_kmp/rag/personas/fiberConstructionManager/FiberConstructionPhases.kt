package com.app.backendplug_kmp.rag.personas.fiberConstructionManager

/**
    * Maps each of the 10 fiber construction phases to targeted search terms for
    * live open-data catalogs (Socrata, ArcGIS Hub, data.gov). Works for any city
    * or county — the caller supplies the location, these just supply the topic.
*/
fun fiberConstructionSearchTerms(phase: String, location: String): String = when (phase) {
    "gis_mapping"            -> "$location road centerlines parcels rights-of-way GIS zoning"
    "utility_infrastructure" -> "$location utility poles conduit electric underground infrastructure"
    "existing_fiber"         -> "$location fiber broadband telecommunications conduit middle mile"
    "broadband_demand"       -> "$location broadband households internet coverage homes units demographics"
    "route_engineering"      -> "$location right-of-way easements corridor engineering utility crossing"
    "permitting"             -> "$location encroachment permit street opening construction development services"
    "pole_attachment"        -> "$location utility pole ownership joint pole attachment make-ready"
    "environmental"          -> "$location wetlands flood zone FEMA environmental habitat coastal sensitive"
    "construction_planning"  -> "$location traffic construction public works staging restoration"
    "efit"                   -> "$location fiber splice OTDR cable inventory as-built GIS asset record"
    else                     -> "$location fiber construction $phase"
}