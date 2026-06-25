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
import java.sql.DriverManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
    * End-to-end check: launches the real installed server as a subprocess and
    * drives it with the MCP client SDK over stdio, exactly the way a production
    * MCP host would.

    * Uses query_sql against a throwaway SQLite file so the test is hermetic,
    * no network, and proves the whole path: client -> stdio -> server -> the
    * :core DataSource seam -> a rendered table coming back.
*/
class McpServerStdioTest {

    private val dbFile: File = File.createTempFile("mcp-test", ".db")

    @BeforeTest
    fun seed() {
        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate("CREATE TABLE city (id INTEGER, name TEXT)")
                stmt.executeUpdate("INSERT INTO city VALUES (1, 'Austin')")
                stmt.executeUpdate("INSERT INTO city VALUES (2, 'Denver')")
            }
        }
    }

    @AfterTest
    fun cleanup() {
        dbFile.delete()
    }

    @Test
    fun queriesSqlOverStdio() {
        // produced by :mcpServer:installDist
        val launcher = File("build/install/mcpServer/bin/mcpServer")
        assertTrue(launcher.exists(), "launcher missing — run :mcpServer:installDist")

        // server's stdout is the MCP channel; keep stderr visible, leave
        // stdin/stdout for the transport to own
        val process = ProcessBuilder(launcher.absolutePath)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        try {
            val client = Client(
                Implementation(name = "test-client", version = "1.0.0"),
                ClientOptions()
            )

            // read what the server writes; write to the server's stdin
            val transport = StdioClientTransport(
                process.inputStream.asSource().buffered(),
                process.outputStream.asSink().buffered()
            )

            runBlocking {
                client.connect(transport)

                val result = client.callTool(
                    "query_sql",
                    mapOf(
                        "jdbcUrl" to "jdbc:sqlite:${dbFile.absolutePath}",
                        "statement" to "SELECT id, name FROM city ORDER BY id"
                    )
                )

                val text = (result!!.content.first() as TextContent).text
                assertTrue(text != null && "Austin" in text, "expected the seeded row, got: $text")

                client.close()
            }
        } finally {
            process.destroyForcibly()
        }
    }
}
