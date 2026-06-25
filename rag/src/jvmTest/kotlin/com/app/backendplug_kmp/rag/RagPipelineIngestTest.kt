package com.app.backendplug_kmp.rag

import com.app.backendplug_kmp.core.domain.DataColumn
import com.app.backendplug_kmp.core.domain.DataRow
import com.app.backendplug_kmp.core.domain.DataTable
import com.app.backendplug_kmp.core.net.createHttpClient
import com.app.backendplug_kmp.rag.clients.OllamaClient
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
    * Live (needs Ollama): builds a small DataTable by hand, ingests it, and
    * confirms every row was embedded and stored. Uses the same DataTable shape
    * any real DataSource produces, so this exercises the actual seam.
*/
class RagPipelineIngestTest {

    @Test
    fun ingestsTableRowsIntoIndex() = runBlocking {
        val pipeline = RagPipeline(OllamaClient(createHttpClient()))

        val table = DataTable(
            columns = listOf(DataColumn("name"), DataColumn("role")),
            rows = listOf(
                DataRow(mapOf("name" to "Ada", "role" to "mathematician")),
                DataRow(mapOf("name" to "Grace", "role" to "admiral"))
            )
        )

        pipeline.ingest(table)

        assertEquals(2, pipeline.indexedCount, "each row should be embedded and indexed")
    }
}
