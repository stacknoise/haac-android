package com.stacknoise.haac.feature.entities.domain

/**
 * An entity that can be added to rooms (M-04): its [domain], its [tile] (name, icon, state, default size) and
 * the rooms it is in already ([roomIds]).
 */
data class CatalogEntry(val domain: String, val tile: Tile, val roomIds: Set<String>) {
    /** The entity's id. */
    val entityId: String get() = tile.entityId
}
