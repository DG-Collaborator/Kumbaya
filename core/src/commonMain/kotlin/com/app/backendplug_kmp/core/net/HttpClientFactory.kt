package com.app.backendplug_kmp.core.net

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
    * Builds the shared Ktor client used by HTTP-based sources.

    * No expect/actual engine is declared: with one engine on each platform's
    * classpath (CIO=desktop, OkHttp=android, Darwin=ios, in
    * build.gradle.kts), Ktor selects it automatically. Common code stays
    * engine-agnostic.
*/
fun createHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        val json = Json {
            // a generic source can't know every field in advance, so never
            // fail on keys we didn't model
            ignoreUnknownKeys = true

            // tolerate loosely-formatted JSON from arbitrary endpoints
            isLenient = true
        }
        // register for standard JSON and for servers that mislabel it as text
        json(json)
        json(json, contentType = ContentType.Text.Plain)
    }
}
