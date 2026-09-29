package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.assignment.TileSize

/**
 * The edit layout of a room before *Done* (concept 7.2, M-06, M-07): [tiles] in their new order and size,
 * [removed] entities taken out. Nothing is saved until *Done*; *Close* discards the draft.
 */
data class LayoutDraft(val tiles: List<Tile>, val removed: Set<String> = emptySet(), val changed: Boolean = false) {
    /** Moves the tile at [from] to position [to]; other tiles close the gap. */
    fun move(from: Int, to: Int): LayoutDraft {
        if (from == to || from !in tiles.indices || to !in tiles.indices) return this
        val list = tiles.toMutableList()
        list.add(to, list.removeAt(from))
        return copy(tiles = list, changed = true)
    }

    /** Gives tile [entityId] the size [size]. */
    fun resize(entityId: String, size: TileSize): LayoutDraft {
        if (tiles.none { it.entityId == entityId && it.size != size }) return this
        return copy(tiles = tiles.map { if (it.entityId == entityId) it.copy(size = size) else it }, changed = true)
    }

    /** Takes tile [entityId] out of the room. */
    fun remove(entityId: String): LayoutDraft =
        copy(tiles = tiles.filterNot { it.entityId == entityId }, removed = removed + entityId, changed = true)

    /**
     * Takes names and states from the room's current [current] tiles (e.g. after *Rename*) and keeps the draft's
     * order, sizes and removals; tiles added meanwhile come at the end, tiles gone meanwhile drop out.
     */
    fun refresh(current: List<Tile>): LayoutDraft {
        val byId = current.associateBy { it.entityId }
        val kept = tiles.mapNotNull { draft -> byId[draft.entityId]?.copy(size = draft.size) }
        val known = tiles.map { it.entityId }.toSet() + removed
        return copy(tiles = kept + current.filter { it.entityId !in known })
    }

    /** Order and sizes to save. */
    fun arrangement(): List<Pair<String, TileSize>> = tiles.map { it.entityId to it.size }
}
