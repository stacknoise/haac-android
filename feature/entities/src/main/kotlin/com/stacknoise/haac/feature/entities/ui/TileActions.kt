package com.stacknoise.haac.feature.entities.ui

import com.stacknoise.haac.feature.entities.domain.Tile

/** Callbacks of the tiles of a room grid (M-05, concept 8). */
internal class TileActions(
    /** Tap on a [Tile.tappable] tile body: a switch toggles, a withdrawn tile opens its sheet. */
    val onClick: (Tile) -> Unit,
    /** Long press: rename and remove. */
    val onLongClick: (Tile) -> Unit,
    /** The toggle of a switch tile. */
    val onToggle: (Tile) -> Unit,
    /** − (false) or + (true) of a climate tile. */
    val onStep: (Tile, Boolean) -> Unit,
)
