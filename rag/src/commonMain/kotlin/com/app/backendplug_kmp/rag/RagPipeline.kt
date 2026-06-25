package com.app.backendplug_kmp.rag

import com.app.backendplug_kmp.core.domain.DataTable
import com.app.backendplug_kmp.rag.clients.LlmClient

/**
    * Ties the pieces together: ingest rows from any DataSource's DataTable into
    * the vector index, then answer questions grounded in those rows.
    * Embedding, retrieval, and generation all delegate to LlmClient, so the
    * model is swapped by changing the LlmClient passed in, not this class.
*/
class RagPipeline(
    private val llm: LlmClient,
    private val index: InMemoryVectorIndex = InMemoryVectorIndex()
) {

    val indexedCount: Int get() = index.size

    /**
        * Embeds every row of a DataTable and loads it into the index. Each row
        * is rendered as "key: value | key: value ..." so the embedding captures
        * both the column names and the values, which makes later retrieval
        * match on meaning rather than raw position.
    */
    suspend fun ingest(table: DataTable) {
        for (row in table.rows) {
            val text = table.columns.joinToString(" | ") { column ->
                "${column.key}: ${row.value(column.key)}"
            }
            val vector = llm.embed(text)
            index.add(text, vector)
        }
    }

    /**
        * The RAG query. Embeds the question, pulls the most similar rows from
        * the index, and asks the llm to answer using only those rows as context,
        * so the answer is grounded in the ingested source rather than the
        * model's training data.
    */
    suspend fun answer(question: String, topK: Int = 4): String {
        val queryVector = llm.embed(question)
        val context = index.search(queryVector, topK)
        return llm.generate(buildPrompt(question, context))
    }

    /**
        * Builds the grounded prompt: the retrieved rows as context, then the
        * question, with an instruction to stay within the data.
    */
    private fun buildPrompt(question: String, context: List<String>): String {
        val dataBlock = if (context.isEmpty()) "(no data)" else context.joinToString("\n")
        return """
              Answer the question using only the data rows below.
              If the answer is not in the data, say you don't know.
  
              Data:
              $dataBlock
  
              Question: $question
              Answer:
          """.trimIndent()
    }
}