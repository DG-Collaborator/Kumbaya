package com.app.backendplug_kmp

import com.app.backendplug_kmp.core.net.createHttpClient
import com.app.backendplug_kmp.core.source.JsonTableSource
import com.app.backendplug_kmp.core.source.SourceQuery
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
    * Live, end-to-end check that JsonTableSource can hit a real endpoint and
    * turn arbitrary JSON into a DataTable, before any UI exists.

    * Uses JSONPlaceholder (/posts), a public fake REST API that returns a bare
    * array of flat objects, exactly shape 1 of extractRecords.
*/
class JsonTableSourceLiveTest {

    @Test
    fun pullsAndParsesPublicJsonArray() = runBlocking {
        // arrange: the universal source wired to the shared HTTP client,
        // identical to what the welcome screen will construct later
        val source = JsonTableSource(createHttpClient())

        // act: the same single call the UI will make, just a URL
        val table = source.fetch(SourceQuery("https://jsonplaceholder.typicode.com/posts"))

        // assert: rows came back and columns were inferred from the data itself
        assertFalse(table.isEmpty, "expected rows back from /posts")
        assertTrue(table.columns.isNotEmpty(), "expected inferred columns")

        // /posts objects are { userId, id, title, body }, so 'title' must appear
        val keys = table.columns.map { it.key }
        assertTrue("title" in keys, "expected a 'title' column, got $keys")

        // sanity: a real cell value resolves for an inferred column
        val firstTitle = table.rows.first().value("title")
        assertTrue(firstTitle.isNotBlank(), "expected first row to have a title")

        // printed so we can eyeball the shape during the walkthrough
        println("columns  = $keys")
        println("rowCount = ${table.rows.size}")
        println("row0.title = $firstTitle")
    }
}
