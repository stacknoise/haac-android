package com.stacknoise.haac.core.database.migration

import java.io.File
import java.sql.Connection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Reads the exported Room schemas and compares them with a migrated SQLite database. */
object RoomSchemas {
    /** Table [table] of the exported Room schema [version]. */
    fun table(version: Int, table: String): JsonObject {
        val file = File("schemas/com.stacknoise.haac.core.database.HaacDatabase/$version.json")
        val root = Json.parseToJsonElement(file.readText()).jsonObject.getValue("database").jsonObject
        return root.getValue("entities").jsonArray.map { it.jsonObject }
            .single { it.getValue("tableName").jsonPrimitive.content == table }
    }

    /** Creates table [table] as Room schema [version] defines it. */
    fun create(db: Connection, version: Int, table: String) {
        val sql = table(version, table).getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table)
        db.createStatement().use { it.execute(sql) }
    }

    /** Column name → (affinity, not null, default) of [table] in Room schema [version]. */
    fun expectedColumns(version: Int, table: String): Map<String, List<Any?>> =
        table(version, table).getValue("fields").jsonArray.map { it.jsonObject }.associate { field ->
            field.getValue("columnName").jsonPrimitive.content to listOf(
                field.getValue("affinity").jsonPrimitive.content,
                field["notNull"]?.jsonPrimitive?.boolean ?: false,
                field["defaultValue"]?.jsonPrimitive?.content,
            )
        }

    /** The same map for [table] as it exists in [db]. */
    fun actualColumns(db: Connection, table: String): Map<String, List<Any?>> =
        db.createStatement().use { statement ->
            statement.executeQuery("PRAGMA table_info($table)").use { result ->
                buildMap {
                    while (result.next()) {
                        val notNull = result.getInt("notnull") == 1
                        val column = listOf(result.getString("type"), notNull, result.getString("dflt_value"))
                        put(result.getString("name"), column)
                    }
                }
            }
        }
}
