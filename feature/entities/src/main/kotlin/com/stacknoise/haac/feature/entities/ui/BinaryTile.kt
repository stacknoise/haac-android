package com.stacknoise.haac.feature.entities.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.database.assignment.TileSize
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.BinaryLevel
import com.stacknoise.haac.feature.entities.domain.Tile
import com.stacknoise.haac.feature.entities.domain.TileContent

/** The level a binary sensor tile is lit with, or null while it is off, not available or withdrawn (concept 8.6). */
internal fun Tile.litLevel(): BinaryLevel? = (content as? TileContent.Binary)?.level?.takeIf { active }

/** True if the tile is drawn as "on": a switched-on switch or a binary sensor that is lit (*Active* or *Alarm*). */
internal fun Tile.lit(): Boolean = (content as? TileContent.Switch)?.on == true && active || litLevel() != null

/** Colour of the state text: error colour for an alarm, accent when [on], else secondary. */
@Composable
internal fun stateColor(on: Boolean, alarm: Boolean): Color = when {
    alarm -> HaacColors.Danger
    on -> HaacColors.Accent
    else -> secondaryColor()
}

/** The line *since 14:32* or *since 2 h* of a binary sensor tile larger than 1×1 (concept 8.6). */
@Composable
internal fun BinarySince(tile: Tile) {
    val since = (tile.content as? TileContent.Binary)?.since
    if (since == null || !tile.available || tile.size == TileSize.SMALL) return
    Text(
        stringResource(R.string.binary_since, sinceText(since)),
        style = MaterialTheme.typography.bodySmall,
        color = secondaryColor(),
        maxLines = 1,
    )
}
