package com.stacknoise.haac.feature.entities.ui.tile

import android.text.format.DateUtils
import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.Tile
import com.stacknoise.haac.feature.entities.domain.TileContent
import com.stacknoise.haac.feature.entities.domain.TileIcon

/** [at] relative to now, e.g. "5 minutes ago" or "In 3 hours" (concept 8.3). */
internal fun relativeTime(at: Long): String =
    DateUtils.getRelativeTimeSpanString(at, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

/** On, Off, Unavailable, a sensor value with unit or a plain state; also used by the picker (M-04). */
@Composable
internal fun stateText(tile: Tile): String {
    if (!tile.available) return stringResource(R.string.tile_unavailable)
    return when (val content = tile.content) {
        is TileContent.Switch -> stringResource(if (content.on == true) R.string.tile_on else R.string.tile_off)
        is TileContent.Sensor -> listOfNotNull(content.value, content.unit).joinToString(" ")
        is TileContent.Timestamp -> relativeTime(content.at)
        is TileContent.Climate -> content.target ?: content.current.orEmpty()
        is TileContent.Binary -> stringResource(content.text)
        is TileContent.Other -> content.state
    }
}

/** Drawable of a [TileIcon]. */
@DrawableRes
internal fun iconOf(icon: TileIcon): Int = when (icon) {
    TileIcon.SWITCH -> R.drawable.ic_entities_switch
    TileIcon.OUTLET -> R.drawable.ic_entities_outlet
    TileIcon.SENSOR -> R.drawable.ic_entities_sensor
    TileIcon.TEMPERATURE, TileIcon.CLIMATE -> R.drawable.ic_entities_temperature
    TileIcon.HUMIDITY -> R.drawable.ic_entities_humidity
    TileIcon.POWER, TileIcon.ENERGY -> R.drawable.ic_entities_power
    TileIcon.BATTERY -> R.drawable.ic_entities_battery
    TileIcon.BINARY -> R.drawable.ic_entities_binary
    TileIcon.DOOR -> R.drawable.ic_entities_door
    TileIcon.MOTION -> R.drawable.ic_entities_motion
    TileIcon.MOISTURE -> R.drawable.ic_entities_water
    TileIcon.LOCK -> R.drawable.ic_entities_lock
    TileIcon.ALARM -> R.drawable.ic_entities_warning
}

/** Secondary text colour of a tile. */
@Composable
internal fun secondaryColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant
