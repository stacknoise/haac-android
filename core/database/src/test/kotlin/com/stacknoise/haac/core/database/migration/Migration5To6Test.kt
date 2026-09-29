package com.stacknoise.haac.core.database.migration

import java.sql.Connection
import java.sql.DriverManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Runs [Migration5To6] on SQLite, compares it with Room schema 6 and checks the cascades (concept 7.2, 12). */
class Migration5To6Test {
    private val db: Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    @AfterEach
    fun close() = db.close()

    private fun exec(sql: String) = db.createStatement().use { it.execute(sql) }

    private fun migrate() {
        listOf("server", "home", "floor", "room").forEach { RoomSchemas.create(db, 5, it) }
        Migration5To6.statements.forEach(::exec)
        exec("PRAGMA foreign_keys = ON")
    }

    @Test
    fun `new tables match Room schema 6`() {
        migrate()
        listOf("room_entity", "entity_alias").forEach { table ->
            assertEquals(RoomSchemas.expectedColumns(6, table), RoomSchemas.actualColumns(db, table), table)
        }
    }

    @Test
    fun `assignments go with their room, aliases with their instance`() {
        migrate()
        RoomSchemas.insertServer(db, "s1")
        exec("INSERT INTO home (id, server_id, name, sort_order) VALUES ('h1', 's1', 'Main', 0)")
        exec("INSERT INTO room (id, home_id, floor_id, name, sort_order) VALUES ('r1', 'h1', NULL, 'Kitchen', 0)")
        exec(
            "INSERT INTO room_entity (room_id, entity_id, sort_order, tile_size, added_at) " +
                "VALUES ('r1', 'switch.a', 1, 'SMALL', 1), ('r1', 'climate.b', 2, 'LARGE', 1)",
        )
        exec("INSERT INTO entity_alias (server_id, entity_id, alias) VALUES ('s1', 'switch.a', 'Lamp')")

        exec("DELETE FROM room WHERE id = 'r1'")
        assertEquals(0, RoomSchemas.count(db, "room_entity"))
        exec("DELETE FROM server WHERE id = 's1'")
        assertEquals(0, RoomSchemas.count(db, "entity_alias"))
    }
}
