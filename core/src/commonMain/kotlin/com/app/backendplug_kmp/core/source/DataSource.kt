package com.app.backendplug_kmp.core.source

import com.app.backendplug_kmp.core.domain.DataTable

/**
    * The UI depends only on this interface, never on a concrete source. Swap
    * the implementation (JSON over HTTP for now; ArcGIS, CSV, or a DB)
    * and nothing above this line changes.

    * Takes a SourceQuery, not a raw URL, so non-HTTP sources fit the same
    * seam.
*/
interface DataSource {
    suspend fun fetch(query: SourceQuery): DataTable
}