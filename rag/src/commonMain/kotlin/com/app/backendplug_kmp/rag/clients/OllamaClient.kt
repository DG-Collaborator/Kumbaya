package com.app.backendplug_kmp.rag.clients

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
    * Minimal client for a local Ollama server. Two calls power the RAG loop:
    * embed text into a vector, and generate an answer from a prompt.

    * Reuses the shared core HttpClient, so JSON (de)serialization is already
    * configured; only the Ollama request/response shapes live here.
*/
class OllamaClient(
    private val client: HttpClient,
    private val baseUrl: String = "http://localhost:11434",
    private val embedModel: String = "nomic-embed-text",
    private val generateModel: String = "llama3.2"
) : LlmClient {

    /**
        * Turns text into an embedding vector. /api/embed returns one vector per
        * input; we send a single input, so we take the first.
    */
    override suspend fun embed(text: String): List<Float> {
        val response: EmbedResponse = client.post("$baseUrl/api/embed") {
            contentType(ContentType.Application.Json)
            setBody(EmbedRequest(model = embedModel, input = text))
        }.body()
        return response.embeddings.firstOrNull().orEmpty()
    }

    /**
        * Generates a completion for a prompt. stream = false so Ollama returns
        * one JSON object with the whole answer in "response".
    */
    override suspend fun generate(prompt: String, systemPrompt: String?): String {
        val response: GenerateResponse = client.post("$baseUrl/api/generate") {
            contentType(ContentType.Application.Json)
            setBody(GenerateRequest(model = generateModel, prompt = prompt, stream = false, system = systemPrompt))
        }.body()
        return response.response
    }
}

@Serializable
private data class EmbedRequest(val model: String, val input: String)
@Serializable
private data class EmbedResponse(val embeddings: List<List<Float>>)

@Serializable
private data class GenerateRequest(val model: String, val prompt: String, val stream: Boolean, val system: String? = null)

@Serializable
private data class GenerateResponse(val response: String)