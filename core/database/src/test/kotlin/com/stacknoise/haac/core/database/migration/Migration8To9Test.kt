package com.stacknoise.haac.core.database.migration

import java.sql.Connection
import java.sql.DriverManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Runs [Migration8To9] on SQLite and compares it with Room schema 9 (concept 12, 19.12). */
class Migration8To9Test {
    private val db: Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    @AfterEach
    fun close() = db.close()

    private fun exec(sql: String) = db.createStatement().use { it.execute(sql) }

    private fun text(sql: String): String? = db.createStatement().use { statement ->
        statement.executeQuery(sql).use { result -> result.next(); result.getString(1) }
    }

    @Test
    fun `schedule matches Room schema 9`() {
        RoomSchemas.create(db, 8, "server")
        RoomSchemas.create(db, 8, "schedule")
        Migration8To9.statements.forEach(::exec)
        assertEquals(RoomSchemas.expectedColumns(9, "schedule"), RoomSchemas.actualColumns(db, "schedule"))
    }

    @Test
    fun `existing schedules get an empty list of upcoming runs`() {
        RoomSchemas.create(db, 8, "server")
        RoomSchemas.create(db, 8, "schedule")
        RoomSchemas.insertServer(db, "s1")
        exec(
            "INSERT INTO schedule (server_id, schedule_id, owner, own, name, enabled, when_type, days, action, " +
                "entity_ids, created_at, updated_at, synced_at) VALUES ('s1', 'a', 'u1', 1, 'Light', 1, 'time', " +
                "31, 'turn_on', '[\"switch.a\"]', 1, '2026-10-01T05:12:00+00:00', 2)",
        )
        Migration8To9.statements.forEach(::exec)
        assertEquals("[]", text("SELECT upcoming_runs FROM schedule"))
        assertEquals(1, RoomSchemas.count(db, "schedule"))
    }
}
