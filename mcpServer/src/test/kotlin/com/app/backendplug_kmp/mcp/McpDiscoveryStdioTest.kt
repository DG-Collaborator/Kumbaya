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
 * find_dataset is now backed by live catalog search (no static registry), so
 * this only asserts a real, non-empty match came back — the specific dataset
 * depends on what's indexed live in Socrata/ArcGIS Hub/data.gov at test time.
 * No network mocking; needs live internet access.
 */
class McpDiscoveryStdioTest {

    @Test
    fun findsDatasetByDescription() {
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
                    mapOf("description" to "us highway and freeway route data")
                )
                val text = (result!!.content.first() as TextContent).text
                assertTrue(
                    text != null && text.isNotBlank() && text != "No matching dataset found.",
                    "expected a live catalog match, got: $text"
                )
                client.close()
            }
        } finally {
            process.destroyForcibly()
        }
    }
}