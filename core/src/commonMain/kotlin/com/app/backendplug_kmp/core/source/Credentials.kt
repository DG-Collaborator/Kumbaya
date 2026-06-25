package com.app.backendplug_kmp.core.source

/**
    * Optional auth a source may need. Kept platform-neutral in commonMain so
    * any source (DB, an authenticated REST API) can accept it.
*/
data class Credentials(
    val username: String,
    val password: String
)