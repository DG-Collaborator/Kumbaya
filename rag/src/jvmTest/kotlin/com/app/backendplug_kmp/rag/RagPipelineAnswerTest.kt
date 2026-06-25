package com.app.backendplug_kmp.rag

import com.app.backendplug_kmp.core.domain.DataColumn
import com.app.backendplug_kmp.core.domain.DataRow
import com.app.backendplug_kmp.core.domain.DataTable
import com.app.backendplug_kmp.core.net.createHttpClient
import com.app.backendplug_kmp.rag.clients.OllamaClient
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/**
    * Live (needs Ollama): ingests a small table, then asks a question whose
    * answer only exists in that data, proving the full loop: embed, retrieve,
    * generate, returns a grounded answer.
*/
class RagPipelineAnswerTest {

    @Test
    fun answersFromIngestedData() = runBlocking {
        val pipeline = RagPipeline(OllamaClient(createHttpClient()))

        val table = DataTable(
            columns = listOf(DataColumn("name"), DataColumn("role")),
            rows = listOf(
                DataRow(mapOf("name" to "Ada", "role" to "mathematician")),
                DataRow(mapOf("name" to "Grace", "role" to "admiral"))
            )
        )
        pipeline.ingest(table)

        val answer = pipeline.answer("What is Ada's role?")
        println("answer = $answer")

        assertTrue(
            answer.contains("mathematician", ignoreCase = true),
            "expected a grounded answer mentioning 'mathematician', got: $answer"
        )
    }
}
