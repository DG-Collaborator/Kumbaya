package com.app.backendplug_kmp

import com.app.backendplug_kmp.core.source.SourceQuery
import com.app.backendplug_kmp.core.source.SqlDataSource
import kotlinx.coroutines.runBlocking
import java.io.File
import java.sql.DriverManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
    * Verifies SqlDataSource against a throwaway on-disk SQLite database.

    * An on-disk file (not :memory:) is used so the seeding connection and the
    * SqlDataSource's own connection observe the same data.
*/
class SqlDataSourceTest {

    private val dbFile: File = File.createTempFile("backendplug-test", ".db")
    private val url: String get() = "jdbc:sqlite:${dbFile.absolutePath}"

    @BeforeTest
    fun seed() {
        DriverManager.getConnection(url).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate("CREATE TABLE city (id INTEGER, name TEXT, population INTEGER)")
                stmt.executeUpdate("INSERT INTO city VALUES (1, 'Austin', 980000)")
                stmt.executeUpdate("INSERT INTO city VALUES (2, 'Denver', 715000)")
            }
        }
    }

    @AfterTest
    fun cleanup() {
        dbFile.delete()
    }

    @Test
    fun readsTableFromSqlite() = runBlocking {
        val source = SqlDataSource()
        val table = source.fetch(
            SourceQuery(
                address = url,
                statement = "SELECT id, name, population FROM city ORDER BY id"
            )
        )

        // columns inferred from the result set, in select order
        assertEquals(listOf("id", "name", "population"), table.columns.map { it.key })

        // two seeded rows, values read as text
        assertEquals(2, table.rows.size)
        assertEquals("Austin", table.rows.first().value("name"))
        assertTrue(table.rows.any { it.value("name") == "Denver" })
    }
}