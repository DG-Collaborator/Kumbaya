package com.app.backendplug_kmp.core.source

import com.app.backendplug_kmp.core.domain.DataColumn
import com.app.backendplug_kmp.core.domain.DataRow
import com.app.backendplug_kmp.core.domain.DataTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.use

/**
    * The SQL counterpart to JsonTableSource: point it at a JDBC URL with a
    * SELECT statement and it produces a DataTable, inferring the columns from
    * the result set's own metadata.

    * Like the JSON source, it models no specific schema. Whatever columns the
    * query returns become the table's columns; every value is read as text so
    * the UI never reasons about SQL types.

    * JVM-only on purpose: JDBC is a Java API, so this can't live in commonMain.
    * The DataSource interface above it is shared; only this implementation is
    * platform-bound.
*/
class SqlDataSource : DataSource {

    override suspend fun fetch(query: SourceQuery): DataTable = withContext(Dispatchers.IO) {
        // statement to run
        val statement = query.statement

        // use { } closes the connection/statement/result set even on error
        openConnection(query).use { connection ->
            connection.createStatement().use { sqlStatement ->
                sqlStatement.executeQuery(statement).use { resultSet ->
                    readTable(resultSet)
                }
            }
        }
    }

    /**
        * Opens a JDBC connection to the query's address. The driver is chosen
        * by the URL scheme (jdbc:sqlite:, jdbc:postgresql:, ...), so swapping
        * databases is a URL change, not a code change.

        * Credentials are passed only when supplied.
    */
    private fun openConnection(query: SourceQuery): Connection {
        val creds = query.credentials
        return if (creds != null) {
            DriverManager.getConnection(query.address, creds.username, creds.password)
        } else {
            DriverManager.getConnection(query.address)
        }
    }

    /**
        * Turns a JDBC ResultSet into the generic DataTable.

        * Columns come from ResultSetMetaData in result order; getColumnLabel
        * honors any "AS" alias. Each value is read with getString, which
        * coerces numbers/dates/etc. to text and yields null for SQL NULL,
        * rendered as "".

        * Note: duplicate column labels (possible with joins) collapse to one
        * key, an accepted simplification of this generic model.
    */
    private fun readTable(resultSet: ResultSet): DataTable {
        val meta = resultSet.metaData
        val keys = (1..meta.columnCount).map { meta.getColumnLabel(it) }
        val columns = keys.map { DataColumn(key = it) }

        val rows = buildList {
            while (resultSet.next()) {
                val cells = keys.withIndex().associate { (index, key) ->
                    key to (resultSet.getString(index + 1) ?: "")
                }
                add(DataRow(cells))
            }
        }

        return DataTable(columns = columns, rows = rows)
    }
}