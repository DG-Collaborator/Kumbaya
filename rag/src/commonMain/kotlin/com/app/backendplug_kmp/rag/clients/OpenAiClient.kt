package com.app.backendplug_kmp.rag.clients

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

/**
    * LlmClient backed by the OpenAI API. Uses text-embedding-3-small for
    * embeddings and gpt-4o-mini for generation by default, both can be
    * overridden at construction to target any OpenAI-compatible model or endpoint.

    * Reuses the shared core HttpClient; the API key is added per-request so
    * one HttpClient instance can serve both Ollama and OpenAI calls side by side.
*/
class OpenAiClient(
    private val client: HttpClient,
    private val apiKey: String,
    private val embedModel: String = "text-embedding-3-small",
    private val generateModel: String = "gpt-4o-mini"
) : LlmClient {

    override suspend fun embed(text: String): List<Float> {
        val httpResponse = client.post("https://api.openai.com/v1/embeddings") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(OpenAiEmbedRequest(model = embedModel, input = text))
        }
        if (!httpResponse.status.isSuccess()) {
            error("OpenAI embeddings request failed (${httpResponse.status.value}) — check that OPENAI_API_KEY is set and has active billing/quota.")
        }
        val response: OpenAiEmbedResponse = httpResponse.body()
        return response.data.firstOrNull()?.embedding.orEmpty()
    }

    override suspend fun generate(prompt: String, systemPrompt: String?): String {
        val messages = buildList {
            if (systemPrompt != null) add(OpenAiChatMessage(role = "system", content = systemPrompt))
            add(OpenAiChatMessage(role = "user", content = prompt))
        }
        val httpResponse = client.post("https://api.openai.com/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(OpenAiChatRequest(model = generateModel, messages = messages))
        }
        if (!httpResponse.status.isSuccess()) {
            error("OpenAI request failed (${httpResponse.status.value}) — check that OPENAI_API_KEY is set correctly.")
        }
        val response: OpenAiChatResponse = httpResponse.body()
        return response.choices.firstOrNull()?.message?.content.orEmpty()
    }
}

@Serializable
private data class OpenAiEmbedRequest(val model: String, val input: String)

@Serializable
private data class OpenAiEmbedResponse(val data: List<OpenAiEmbedData>)

@Serializable
private data class OpenAiEmbedData(val embedding: List<Float>)

@Serializable
private data class OpenAiChatRequest(val model: String, val messages: List<OpenAiChatMessage>)

@Serializable
private data class OpenAiChatMessage(val role: String, val content: String)

@Serializable
private data class OpenAiChatResponse(val choices: List<OpenAiChatChoice> = emptyList())

@Serializable
private data class OpenAiChatChoice(val message: OpenAiChatMessage)