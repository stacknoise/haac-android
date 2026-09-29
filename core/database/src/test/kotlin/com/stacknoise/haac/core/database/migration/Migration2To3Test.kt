package com.stacknoise.haac.core.database.migration

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** Runs [Migration2To3] on SQLite and compares the result with the exported Room schema 3. */
class Migration2To3Test {
    private val db: Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    @AfterEach
    fun close() = db.close()

    private fun exec(sql: String) = db.createStatement().use { it.execute(sql) }

    private fun count(table: String): Int = db.createStatement().use { statement ->
        statement.executeQuery("SELECT COUNT(*) FROM $table").use { it.next(); it.getInt(1) }
    }

    private fun migrate() {
        RoomSchemas.create(db, 2, "server")
        exec(
            "INSERT INTO server (id, display_name, accent_color, ha_user_name, ha_version, bridge_api_version, " +
                "last_active_at) VALUES ('s1', 'Home', 1, 'anna', '2026.9.0', 1, 5)",
        )
        Migration2To3.statements.forEach(::exec)
    }

    private fun insertEntity(serverId: String) = exec(
        "INSERT INTO exposed_entity (server_id, entity_id, domain, ha_name, supported_features, status) " +
            "VALUES ('$serverId', 'switch.x', 'switch', 'X', 0, 'ACTIVE')",
    )

    @Test
    fun `new table matches Room schema 3 and server is unchanged`() {
        migrate()
        assertEquals(RoomSchemas.expectedColumns(3, "exposed_entity"), RoomSchemas.actualColumns(db, "exposed_entity"))
        assertEquals(RoomSchemas.expectedColumns(3, "server"), RoomSchemas.actualColumns(db, "server"))
        assertEquals(1, count("server"))
    }

    @Test
    fun `entities belong to a server and go with it`() {
        migrate()
        exec("PRAGMA foreign_keys = ON")
        insertEntity("s1")
        assertThrows<SQLException> { insertEntity("unknown") }
        exec("DELETE FROM server WHERE id = 's1'")
        assertEquals(0, count("exposed_entity"))
    }
}
