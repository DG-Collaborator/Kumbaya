package com.app.backendplug_kmp.mcp

import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.ClientOptions
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
    * Capstone end-to-end: launches the real server and calls find_dataset over
    * stdio, proving natural-language discovery works through the MCP boundary.
    * Needs Ollama (resolver embeddings); no network (resolving returns a URL).
*/
class McpDiscoveryStdioTest {

    @Test
    fun findsHighwayDatasetByDescription() {
        val launcher = File("build/install/mcpServer/bin/mcpServer")
        assertTrue(launcher.exists(), "launcher missing — run :mcpServer:installDist")

        val process = ProcessBuilder(launcher.absolutePath)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        try {
            val client = Client(
                Implementation(name = "test-client", version = "1.0.0"),
                ClientOptions()
            )
            val transport = StdioClientTransport(
                process.inputStream.asSource().buffered(),
                process.outputStream.asSink().buffered()
            )

            runBlocking {
                client.connect(transport)
                val result = client.callTool(
                    "find_dataset",
                    mapOf("description" to "find me recent us highway data")
                )
                val text = (result!!.content.first() as TextContent).text
                assertTrue(
                    text != null && "USA Freeway System" in text,
                    "expected the freeway dataset, got: $text"
                )
                client.close()
            }
        } finally {
            process.destroyForcibly()
        }
    }
}