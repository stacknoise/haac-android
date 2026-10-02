package com.stacknoise.haac.core.database.migration

import java.sql.Connection
import java.sql.DriverManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Runs [Migration6To7] on SQLite, compares it with Room schema 7 and checks the cascade (concept 12, 19.7). */
class Migration6To7Test {
    private val db: Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    @AfterEach
    fun close() = db.close()

    private fun exec(sql: String) = db.createStatement().use { it.execute(sql) }

    private fun migrate() {
        listOf("server", "notification").forEach { RoomSchemas.create(db, 6, it) }
        Migration6To7.statements.forEach(::exec)
        exec("PRAGMA foreign_keys = ON")
    }

    @Test
    fun `new table and column match Room schema 7`() {
        migrate()
        listOf("schedule", "notification").forEach { table ->
            assertEquals(RoomSchemas.expectedColumns(7, table), RoomSchemas.actualColumns(db, table), table)
        }
    }

    @Test
    fun `existing notifications stay, schedules go with their instance`() {
        RoomSchemas.create(db, 6, "server")
        RoomSchemas.create(db, 6, "notification")
        RoomSchemas.insertServer(db, "s1")
        exec(
            "INSERT INTO notification (server_id, type, count, entity_ids, created_at) " +
                "VALUES ('s1', 'ADDED', 1, '[]', 1)",
        )
        Migration6To7.statements.forEach(::exec)
        exec("PRAGMA foreign_keys = ON")
        assertEquals(1, RoomSchemas.count(db, "notification"))

        exec(
            "INSERT INTO schedule (server_id, schedule_id, owner, own, name, enabled, when_type, days, action, " +
                "entity_ids, created_at, updated_at, synced_at) VALUES ('s1', 'a', 'u1', 1, 'Light', 1, 'time', " +
                "31, 'turn_on', '[\"switch.a\"]', 1, '2026-10-01T05:12:00+00:00', 2)",
        )
        assertEquals(1, RoomSchemas.count(db, "schedule"))
        exec("DELETE FROM server WHERE id = 's1'")
        assertEquals(0, RoomSchemas.count(db, "schedule"))
    }
}
