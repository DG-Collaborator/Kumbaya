package com.app.backendplug_kmp.rag.clients

/**
    * The LLM seam: embed text into a vector, and generate a completion from
    * a prompt. Implement once per model provider; RagPipeline and
    * RegistrySourceResolver depend on this interface, not on any concrete client.
*/
interface LlmClient {
    suspend fun embed(text: String): List<Float>
    suspend fun generate(prompt: String, systemPrompt: String? = null): String
}