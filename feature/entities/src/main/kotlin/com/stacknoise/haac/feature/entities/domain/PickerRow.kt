package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.layout.Places

/**
 * One row of *Add entities* (M-04): [inRoom] entities are already in this room and cannot be picked again
 * (concept 7.2); [otherRoom] names a room it is in elsewhere, only as a hint (15.5 item 2).
 */
data class PickerRow(val entry: CatalogEntry, val inRoom: Boolean, val otherRoom: String?)

/** The domains of v1 first, in the order of the tabs (M-04); others follow alphabetically. */
private val TabOrder = listOf("switch", "sensor", "climate")

/** Position of [domain] among the tabs: the v1 domains first. */
private fun tabIndex(domain: String): Int = TabOrder.indexOf(domain).let { if (it < 0) TabOrder.size else it }

/** The domain tabs for [entries]: the v1 domains in their order, then any others. */
fun pickerDomains(entries: List<CatalogEntry>): List<String> =
    entries.map { it.domain }.distinct().sortedWith(compareBy({ tabIndex(it) }, { it }))

/**
 * The rows of tab [domain] for room [roomId] whose name or `entity_id` contains [filter] (ignoring case), with
 * the room hints taken from [places].
 */
fun pickerRows(
    entries: List<CatalogEntry>,
    roomId: String,
    places: Places,
    domain: String?,
    filter: String,
): List<PickerRow> {
    val query = filter.trim()
    return entries
        .filter { it.domain == domain }
        .filter { query.isEmpty() || it.tile.name.contains(query, true) || it.entityId.contains(query, true) }
        .map { entry ->
            val elsewhere = entry.roomIds.filter { it != roomId }.firstNotNullOfOrNull { places.room(it)?.name }
            PickerRow(entry, roomId in entry.roomIds, elsewhere)
        }
}
