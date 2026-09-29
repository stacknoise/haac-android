package com.stacknoise.haac.feature.entities.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.EntityDetail
import com.stacknoise.haac.feature.entities.domain.ReadingKind
import com.stacknoise.haac.feature.entities.domain.TileContent
/** The big value: the HVAC mode of a climate entity, else the tile's state; a time also absolute. */
@Composable
internal fun primaryValue(detail: EntityDetail): String {
    val content = detail.tile.content
    return when {
        !detail.tile.available -> stateText(detail.tile)
        content is TileContent.Climate -> detail.state?.let(::modeLabel).orEmpty()
        content is TileContent.Timestamp -> timeText(content.at)
        else -> stateText(detail.tile)
    }
}

/** "5 minutes ago · 29 Sep 2026, 14:05". */
@Composable
internal fun timeText(at: Long): String = stringResource(R.string.detail_time, relativeTime(at), absoluteTime(at))

/** The label of a reading. */
@Composable
internal fun readingLabel(kind: ReadingKind): String = stringResource(
    when (kind) {
        ReadingKind.CURRENT_TEMPERATURE -> R.string.detail_current_temperature
        ReadingKind.CURRENT_HUMIDITY -> R.string.detail_current_humidity
        ReadingKind.HVAC_ACTION -> R.string.detail_hvac_action
    },
)

/** `hvac_action` as a label ("Heating"), numbers as they are. */
internal fun modeOrValue(kind: ReadingKind, value: String): String =
    if (kind == ReadingKind.HVAC_ACTION) modeLabel(value) else value
