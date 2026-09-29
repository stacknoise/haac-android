package com.stacknoise.haac.core.database.migration

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** Runs [Migration4To5] on SQLite, compares it with Room schema 5 and checks trigger and cascades (concept 6). */
class Migration4To5Test {
    private val db: Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    @BeforeEach
    fun migrate() {
        RoomSchemas.create(db, 4, "server")
        Migration4To5.statements.forEach(::exec)
        exec("PRAGMA foreign_keys = ON")
        exec(
            "INSERT INTO server (id, display_name, accent_color, ha_user_name, ha_version, bridge_api_version, " +
                "last_active_at) VALUES ('s1', 'Home', 1, 'anna', '2026.9.0', 1, 5)",
        )
        exec(
            "INSERT INTO home (id, server_id, name, sort_order) " +
                "VALUES ('h1', 's1', 'Main', 0), ('h2', 's1', 'Garden', 1)",
        )
        exec("INSERT INTO floor (id, home_id, name, level, sort_order) VALUES ('f1', 'h1', 'Ground', 0, 0)")
    }

    @AfterEach
    fun close() = db.close()

    private fun exec(sql: String) = db.createStatement().use { it.execute(sql) }

    private fun column(sql: String): List<String?> = db.createStatement().use { statement ->
        statement.executeQuery(sql).use { result -> buildList { while (result.next()) add(result.getString(1)) } }
    }

    @Test
    fun `new tables match Room schema 5`() {
        listOf("home", "floor", "room").forEach { table ->
            assertEquals(RoomSchemas.expectedColumns(5, table), RoomSchemas.actualColumns(db, table), table)
        }
        val indexes = column("SELECT name FROM sqlite_master WHERE type = 'index' AND name LIKE 'index_%' ORDER BY 1")
        val expected = listOf(
            "index_floor_home_id",
            "index_home_server_id",
            "index_room_floor_id",
            "index_room_home_id",
        )
        assertEquals(expected, indexes)
    }

    @Test
    fun `a room cannot link a floor of another home`() {
        exec("INSERT INTO room (id, home_id, floor_id, name, sort_order) VALUES ('r1', 'h1', 'f1', 'Kitchen', 0)")
        exec("INSERT INTO room (id, home_id, floor_id, name, sort_order) VALUES ('r2', 'h2', NULL, 'Shed', 0)")

        assertThrows<SQLException> {
            exec("INSERT INTO room (id, home_id, floor_id, name, sort_order) VALUES ('r3', 'h2', 'f1', 'Bad', 1)")
        }
        assertThrows<SQLException> { exec("UPDATE room SET home_id = 'h2' WHERE id = 'r1'") }
        exec("UPDATE room SET home_id = 'h2', floor_id = NULL WHERE id = 'r1'")
        assertEquals(listOf("h2", "h2"), column("SELECT home_id FROM room ORDER BY id"))
    }

    @Test
    fun `deleting a floor keeps its rooms, deleting a home or server removes everything`() {
        exec("INSERT INTO room (id, home_id, floor_id, name, sort_order) VALUES ('r1', 'h1', 'f1', 'Kitchen', 0)")
        exec("DELETE FROM floor WHERE id = 'f1'")
        assertEquals(listOf(null), column("SELECT floor_id FROM room"))

        exec("DELETE FROM home WHERE id = 'h1'")
        assertEquals(emptyList<String>(), column("SELECT id FROM room"))

        exec("DELETE FROM server WHERE id = 's1'")
        assertEquals(emptyList<String>(), column("SELECT id FROM home"))
    }
}
