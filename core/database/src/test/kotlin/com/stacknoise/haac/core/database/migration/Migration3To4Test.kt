package com.stacknoise.haac.core.database.migration

import java.sql.Connection
import java.sql.DriverManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Runs [Migration3To4] on SQLite and compares the result with the exported Room schema 4. */
class Migration3To4Test {
    private val db: Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    @AfterEach
    fun close() = db.close()

    private fun exec(sql: String) = db.createStatement().use { it.execute(sql) }

    private fun indexes(): List<String> = db.createStatement().use { statement ->
        statement.executeQuery("PRAGMA index_list(notification)").use { result ->
            buildList { while (result.next()) add(result.getString("name")) }
        }
    }

    @Test
    fun `new table and index match Room schema 4, older tables stay`() {
        RoomSchemas.create(db, 3, "server")
        RoomSchemas.create(db, 3, "exposed_entity")
        Migration3To4.statements.forEach(::exec)

        assertEquals(RoomSchemas.expectedColumns(4, "notification"), RoomSchemas.actualColumns(db, "notification"))
        assertEquals(listOf("index_notification_server_id"), indexes())
        assertEquals(RoomSchemas.expectedColumns(4, "exposed_entity"), RoomSchemas.actualColumns(db, "exposed_entity"))
    }

    @Test
    fun `global entries have no instance, instance entries go with it`() {
        RoomSchemas.create(db, 3, "server")
        Migration3To4.statements.forEach(::exec)
        exec("PRAGMA foreign_keys = ON")
        exec(
            "INSERT INTO server (id, display_name, accent_color, ha_user_name, ha_version, bridge_api_version, " +
                "last_active_at) VALUES ('s1', 'Home', 1, 'anna', '2026.9.0', 1, 5)",
        )
        exec(
            "INSERT INTO notification (server_id, type, count, entity_ids, created_at) " +
                "VALUES ('s1', 'ADDED', 1, '[]', 1), (NULL, 'ERROR', 1, '[]', 2)",
        )
        exec("DELETE FROM server WHERE id = 's1'")
        db.createStatement().use { statement ->
            statement.executeQuery("SELECT type FROM notification").use { result ->
                result.next()
                assertEquals("ERROR", result.getString(1))
            }
        }
    }
}
