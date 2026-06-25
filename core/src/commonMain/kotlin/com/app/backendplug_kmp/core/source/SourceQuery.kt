package com.app.backendplug_kmp.core.source

/**
    * A source-neutral descriptor of "what to fetch".

    * address is whatever locates the data for a given source: a URL for the
    * JSON source today, a JDBC connection string for the SQL source next.

    * Stays a plain wrapper on purpose. Future fields (an SQL statement,
    * credentials) can be added here without changing the fetch signature or
    * any call site below it.
*/
data class SourceQuery(
    val address: String,
    // SQL/query text for sources that need it. Null for a plain HTTP GET
    val statement: String? = null,
    // Optional auth for public/anon sources
    val credentials: Credentials? = null
)