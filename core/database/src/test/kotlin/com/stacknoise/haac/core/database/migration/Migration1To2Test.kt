package com.stacknoise.haac.core.database.migration

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Runs [Migration1To2] on SQLite and compares the result with the exported Room schema 2. */
class Migration1To2Test {
    private val db: Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    @AfterEach
    fun close() = db.close()

    /** The `server` table of an exported Room schema. */
    private fun schema(version: Int): JsonObject {
        val file = File("schemas/com.stacknoise.haac.core.database.HaacDatabase/$version.json")
        val root = Json.parseToJsonElement(file.readText()).jsonObject.getValue("database").jsonObject
        return root.getValue("entities").jsonArray.single().jsonObject
    }

    private fun exec(sql: String) = db.createStatement().use { it.execute(sql) }

    private fun insertV1(id: String, url: String, pin: String?) = exec(
        "INSERT INTO server (id, base_url, display_name, accent_color, ha_user_name, ha_version, " +
            "bridge_api_version, pinned_key_hash, last_active_at) " +
            "VALUES ('$id', '$url', 'Home', 1, 'anna', '2026.9.0', 1, ${pin?.let { "'$it'" } ?: "NULL"}, 5)",
    )

    /** Column values of the row [id] after the migration. */
    private fun row(id: String): Map<String, Any?> = db.createStatement().use { statement ->
        statement.executeQuery("SELECT * FROM server WHERE id = '$id'").use { result ->
            result.next()
            (1..result.metaData.columnCount).associate { result.metaData.getColumnName(it) to result.getObject(it) }
        }
    }

    @Test
    fun `http goes to the internal slot, https to the external slot`() {
        exec(schema(1).getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", "server"))
        insertV1("a", "http://192.168.1.10:8123/", pin = null)
        insertV1("b", "https://ha.example.com/", pin = "sha256/abc")

        Migration1To2.statements.forEach(::exec)

        val local = row("a")
        assertEquals("http://192.168.1.10:8123/", local["internal_url"])
        assertNull(local["external_url"])
        assertNull(local["instance_uuid"])
        assertEquals(0, local["always_use_internal"])
        val remote = row("b")
        assertNull(remote["internal_url"])
        assertEquals("https://ha.example.com/", remote["external_url"])
        assertEquals("sha256/abc", remote["external_pinned_key_hash"])
        assertEquals("anna", remote["ha_user_name"])
    }

    @Test
    fun `migrated table matches Room schema 2`() {
        exec(schema(1).getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", "server"))
        Migration1To2.statements.forEach(::exec)

        val expected = schema(2).getValue("fields").jsonArray.map { it.jsonObject }.associate { field ->
            field.getValue("columnName").jsonPrimitive.content to listOf(
                field.getValue("affinity").jsonPrimitive.content,
                field["notNull"]?.jsonPrimitive?.boolean ?: false,
                field["defaultValue"]?.jsonPrimitive?.content,
            )
        }
        val actual = db.createStatement().use { statement ->
            statement.executeQuery("PRAGMA table_info(server)").use { result ->
                buildMap {
                    while (result.next()) {
                        put(
                            result.getString("name"),
                            listOf(
                                result.getString("type"),
                                result.getInt("notnull") == 1,
                                result.getString("dflt_value"),
                            ),
                        )
                    }
                }
            }
        }
        assertEquals(expected, actual)
    }
}
