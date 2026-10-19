package com.stacknoise.haac.core.database.migration

import java.sql.Connection
import java.sql.DriverManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Runs [Migration7To8] on SQLite, compares it with Room schema 8 and checks defaults and cascade (concept 12). */
class Migration7To8Test {
    private val db: Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    @AfterEach
    fun close() = db.close()

    private fun exec(sql: String) = db.createStatement().use { it.execute(sql) }

    private fun text(sql: String): String? = db.createStatement().use { statement ->
        statement.executeQuery(sql).use { result -> result.next(); result.getString(1) }
    }

    private fun createV7(vararg tables: String) = tables.forEach { RoomSchemas.create(db, 7, it) }

    @Test
    fun `changed and new tables match Room schema 8`() {
        createV7("server", "exposed_entity", "schedule")
        Migration7To8.statements.forEach(::exec)
        listOf("server", "exposed_entity", "schedule", "entity_guard").forEach { table ->
            assertEquals(RoomSchemas.expectedColumns(8, table), RoomSchemas.actualColumns(db, table), table)
        }
    }

    @Test
    fun `existing rows get the defaults of the extension`() {
        createV7("server", "exposed_entity", "schedule")
        RoomSchemas.insertServer(db, "s1")
        exec(
            "INSERT INTO exposed_entity (server_id, entity_id, domain, ha_name, supported_features, status) " +
                "VALUES ('s1', 'switch.a', 'switch', 'A', 0, 'ACTIVE')",
        )
        exec(
            "INSERT INTO schedule (server_id, schedule_id, owner, own, name, enabled, when_type, days, action, " +
                "entity_ids, created_at, updated_at, synced_at) VALUES ('s1', 'a', 'u1', 1, 'Light', 1, 'time', " +
                "31, 'turn_on', '[\"switch.a\"]', 1, '2026-10-01T05:12:00+00:00', 2)",
        )
        Migration7To8.statements.forEach(::exec)

        assertEquals("0", text("SELECT disabled FROM exposed_entity"))
        assertEquals(null, text("SELECT disabled_at FROM exposed_entity"))
        assertEquals("weekly", text("SELECT repeat_freq FROM schedule"))
        assertEquals("1", text("SELECT repeat_interval FROM schedule"))
        assertEquals("0", text("SELECT notify FROM schedule"))
        assertEquals("[]", text("SELECT blocked_by FROM schedule"))
        assertEquals("0", text("SELECT expired FROM schedule"))
        assertEquals("[]", text("SELECT next_runs FROM schedule"))
        assertEquals(null, text("SELECT schedule_run_log_id FROM server"))
        assertEquals(null, text("SELECT schedule_run_seq FROM server"))
        assertEquals(0, RoomSchemas.count(db, "entity_guard"))
    }

    @Test
    fun `guards go with their instance`() {
        createV7("server")
        RoomSchemas.insertServer(db, "s1")
        Migration7To8.statements.forEach { if (it.contains("entity_guard")) exec(it) }
        exec("PRAGMA foreign_keys = ON")
        exec("INSERT INTO entity_guard (server_id, entity_id, on_turn_on, on_turn_off) VALUES ('s1', 'switch.a', 1, 1)")
        assertEquals(1, RoomSchemas.count(db, "entity_guard"))
        exec("DELETE FROM server WHERE id = 's1'")
        assertEquals(0, RoomSchemas.count(db, "entity_guard"))
    }
}
