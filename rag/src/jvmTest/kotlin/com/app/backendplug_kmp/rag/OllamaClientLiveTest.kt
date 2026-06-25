package com.app.backendplug_kmp.rag

import com.app.backendplug_kmp.core.net.createHttpClient
import com.app.backendplug_kmp.rag.clients.OllamaClient
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/**
    * Live check against a local Ollama (must be running with the two models
    * pulled). Proves the embed + generate calls round-trip before we build the
    * vector index on top of them.
*/
class OllamaClientLiveTest {

    @Test
    fun embedsAndGenerates() = runBlocking {
        val ollama = OllamaClient(createHttpClient())

        val vector = ollama.embed("hello world")
        assertTrue(vector.isNotEmpty(), "expected an embedding vector from Ollama")
        println("embedding dims = ${vector.size}")   // nomic-embed-text → 768

        val answer = ollama.generate("Reply with exactly one word: pong")
        assertTrue(answer.isNotBlank(), "expected a generated answer")
        println("generate = $answer")
    }
}