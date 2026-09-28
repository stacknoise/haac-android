package com.stacknoise.haac.core.network.http

import com.stacknoise.haac.core.error.UnexpectedException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** String value of [key] in a HA JSON answer, or null if it is missing or JSON null. */
fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

/**
 * Decodes an answer of a server already known to be HA; malformed JSON is HAAC-APP-000.
 * `SerializationException` is an `IllegalArgumentException`, so one catch covers both.
 */
fun <T> Json.decodeOrUnexpected(deserializer: DeserializationStrategy<T>, body: String): T = try {
    decodeFromString(deserializer, body)
} catch (e: IllegalArgumentException) {
    throw UnexpectedException(e)
}
