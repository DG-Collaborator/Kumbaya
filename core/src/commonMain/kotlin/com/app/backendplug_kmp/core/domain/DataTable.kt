package com.app.backendplug_kmp.core.domain

/**
    * This is what makes the backend pluggable: every source
    * produces a DataTable, and the UI renders whatever columns it finds.
*/
data class DataTable(
    val columns: List<DataColumn>,
    val rows: List<DataRow>
) {
    // convenience for the UI's empty state
    val isEmpty: Boolean get() = rows.isEmpty()

    companion object {
        // a typed "nothing yet" value so callers avoid null checks
        val EMPTY = DataTable(emptyList(), emptyList())
    }
}

/**
    * One column. key is the raw field name from the source (used to look up
    * cell values); label is what the header shows. Same by default, label
    * exists so a UI can improve a header without breaking value lookups.
*/
data class DataColumn(
    val key: String,
    val label: String = key
)

/**
    * One row, addressed by column key. Values are pre-stringified: the source
    * layer flattens numbers/booleans/null into display text, so the UI never
    * reasons about JSON types. A missing cell resolves to an empty string.
*/
data class DataRow(
    val cells: Map<String, String>
) {
    fun value(key: String): String = cells[key] ?: ""
}