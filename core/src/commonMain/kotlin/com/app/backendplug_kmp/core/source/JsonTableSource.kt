package com.app.backendplug_kmp.core.source

import com.app.backendplug_kmp.core.domain.DataColumn
import com.app.backendplug_kmp.core.domain.DataRow
import com.app.backendplug_kmp.core.domain.DataTable
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
    * The universal source: point it at any URL that returns JSON and it
    * produces a DataTable, inferring the columns from the data itself.

    * It models no specific schema. Whatever objects the endpoint returns become rows,
    * the union of their keys becomes the columns. Swap the URL on the welcome screen
    * and the same code handles a completely different dataset.
*/
class JsonTableSource(
    private val client: HttpClient
) : DataSource {

    override suspend fun fetch(query: SourceQuery): DataTable {
        /*
           ContentNegotiation (HttpClientFactory) parses the body into a generic
           JSON tree, no predefined data class required This source treats the
           query's address as the URL to GET.
        */
        val root: JsonElement = client.get(query.address).body()

        /*
           step 1: find the records, then flatten any vendor wrapper so their real
           fields surface as columns instead of a single wrapper column
        */
        val records: List<JsonObject> = extractRecords(root).map(::unwrap)
        if (records.isEmpty()) return DataTable.EMPTY

        /*
           step 2: columns = union of keys across all records, first-seen order.
           LinkedHashSet preserves insertion order, so the header follows the
           source's own field order, then appends any keys later rows introduce.
        */
        val columnKeys = LinkedHashSet<String>()
        for (record in records) {
            columnKeys.addAll(record.keys)
        }
        val columns = columnKeys.map { DataColumn(key = it) }

        // step 3: each record becomes a row; absent keys resolve to "" in DataRow
        val rows = records.map { record ->
            val cells = columnKeys.associateWith { key ->
                record[key]?.let { stringify(it) } ?: ""
            }
            DataRow(cells)
        }

        return DataTable(columns = columns, rows = rows)
    }

    /**
        * Locates the array of row-objects within an arbitrary JSON response.

        * Handles the three common shapes:
        *   1. a bare array               [ {...}, {...} ]
        *   2. an object wrapping a list  { "data": [ {...} ] }  (data/results/items/features/...)
        *   3. a single object            { ... }  → one row
    */
    private fun extractRecords(root: JsonElement): List<JsonObject> = when (root) {
        // Already an array, keep only the object entries
        is JsonArray -> root.filterIsInstance<JsonObject>()

        is JsonObject -> {
            // first choice: a well-known data key by name
            val preferredKeys = listOf("features", "data", "results", "items", "records", "rows")
            val byName = preferredKeys.firstNotNullOfOrNull { key ->
                (root[key] as? JsonArray)?.takeIf { arr -> arr.any { it is JsonObject } }
            }

            /*
               fallback: the LARGEST array of objects. Record arrays are almost
               always bigger than incidental metadata arrays, so size is a far
               safer bet than position.
            */
            val recordsArray = byName ?: root.values
                .filterIsInstance<JsonArray>()
                .filter { arr -> arr.any { it is JsonObject } }
                .maxByOrNull { it.size }

            when {
                recordsArray != null -> recordsArray.filterIsInstance<JsonObject>()
                // shape 3: treat the object itself as a one-row table
                else -> listOf(root)
            }
        }

        // a bare primitive isn't tabular, nothing to show
        else -> emptyList()
    }


    /**
        * Flattens a "wrapper" row into a plain field object.

        * Feature services nest the real columns one level down: ArcGIS puts
        * them under "attributes", GeoJSON under "properties". Left alone, the
        * parser would infer a single column named after the wrapper, with each
        * cell holding a JSON blob. Here we promote the inner object to be the row.

        * The two cases are detected differently on purpose:
        *   - "attributes" is unwrapped whenever its value is an object (a strong,
        *     unambiguous ArcGIS convention).
        *   - "properties" is unwrapped only when the row also looks like a
        *     GeoJSON Feature (has a "geometry", or type == "Feature"), so we
        *     don't clobber some unrelated object that merely happens to be
        *     named "properties".
    */
    private fun unwrap(record: JsonObject): JsonObject {
        // ArcGIS feature: { "attributes": { ...real fields... } }
        (record["attributes"] as? JsonObject)?.let { inner ->
            return mergeSiblings(record, wrapperKey = "attributes", inner = inner)
        }

        // GeoJSON feature: { "type": "Feature", "properties": {...}, "geometry": {...} }
        val looksLikeGeoJson =
            (record["type"] as? JsonPrimitive)?.content == "Feature" ||
                    record.containsKey("geometry")
        if (looksLikeGeoJson) {
            (record["properties"] as? JsonObject)?.let { inner ->
                return mergeSiblings(record, wrapperKey = "properties", inner = inner)
            }
        }

        // already flat, nothing to do
        return record
    }

    /**
        * Merges the unwrapped inner object with the row's sibling primitive
        * fields (like a top-level "id" sitting next to "properties").

        * Only primitive siblings are kept.
        * Inner fields win on key collisions, since those are the real columns.
    */
    private fun mergeSiblings(
        record: JsonObject,
        wrapperKey: String,
        inner: JsonObject
    ): JsonObject {
        val siblings = record
            .filterKeys { it != wrapperKey }
            .filterValues { it is JsonPrimitive }
        return JsonObject(siblings + inner)
    }



    /**
        * Flattens a JSON value to display text so the UI never sees raw types.
        * Primitives drop their surrounding quotes; null becomes blank; nested
        * objects/arrays fall back to their compact JSON form.

        * Order matters: JsonNull is a subtype of JsonPrimitive, so it must be
        * checked first or nulls would stringify to the literal "null".
    */
    private fun stringify(element: JsonElement): String = when (element) {
        is JsonNull -> ""
        is JsonPrimitive -> element.content
        else -> element.toString()
    }
}
