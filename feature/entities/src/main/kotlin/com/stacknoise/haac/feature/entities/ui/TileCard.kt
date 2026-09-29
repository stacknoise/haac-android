package com.stacknoise.haac.feature.entities.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.Tile
import com.stacknoise.haac.feature.entities.domain.TileContent
import com.stacknoise.haac.feature.entities.domain.TileIcon

/**
 * One tile of the room grid (M-05, M-08): icon, name and state; "on" switches use the accent container, tiles of
 * withdrawn entities are dashed and struck through (concept 7.4). Controls follow with chapter 8.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TileCard(tile: Tile, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val on = (tile.content as? TileContent.Switch)?.on == true && tile.available && !tile.withdrawn
    val frame = when {
        tile.withdrawn -> Modifier.dashedBorder(colors.outline)
        on -> Modifier
            .background(colors.primaryContainer, HaacShapes.Medium)
            .border(1.dp, colors.primary, HaacShapes.Medium)
        else -> Modifier.background(colors.surface, HaacShapes.Medium)
    }
    Box(
        modifier = modifier
            .clip(HaacShapes.Medium)
            .then(frame)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(16.dp),
    ) {
        when {
            tile.withdrawn -> WithdrawnContent(tile)
            tile.content is TileContent.Climate -> ClimateContent(tile, tile.content)
            else -> PlainContent(tile, on)
        }
    }
}

/** Icon, then value or name and state (switch, sensor, other domains). */
@Composable
private fun PlainContent(tile: Tile, on: Boolean) {
    val accent = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = Modifier.fillMaxSize()) {
        Icon(
            painterResource(iconOf(tile.icon)),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.weight(1f))
        val sensor = tile.content as? TileContent.Sensor
        if (sensor != null && tile.available) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(sensor.value, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
                sensor.unit?.let { Text(" $it", style = MaterialTheme.typography.bodyMedium, maxLines = 1) }
            }
            TileName(tile.name, secondary = true)
        } else {
            TileName(tile.name)
            Text(
                stateText(tile),
                style = MaterialTheme.typography.bodyMedium,
                color = if (on) MaterialTheme.colorScheme.onPrimaryContainer else secondaryColor(),
                maxLines = 1,
            )
        }
    }
}

/** Name, heating mark, target temperature large and the current one below (M-05; dial and ± follow later). */
@Composable
private fun ClimateContent(tile: Tile, climate: TileContent.Climate) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tile.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.weight(1f))
            if (climate.heating && tile.available) {
                Icon(
                    painterResource(R.drawable.ic_entities_heating),
                    stringResource(R.string.tile_heating),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            if (tile.available) climate.target ?: "–" else stringResource(R.string.tile_unavailable),
            style = if (tile.available) MaterialTheme.typography.displaySmall else MaterialTheme.typography.bodyMedium,
        )
        climate.current?.takeIf { tile.available }?.let {
            Text(
                stringResource(R.string.tile_now, it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

/** Inactive tile: icon, struck-through name, hint and warning badge (M-08). */
@Composable
private fun WithdrawnContent(tile: Tile) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Icon(
                painterResource(iconOf(tile.icon)),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                tile.name,
                style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.LineThrough),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(stringResource(R.string.tile_removed), style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
        Icon(
            painterResource(R.drawable.ic_entities_warning),
            contentDescription = stringResource(R.string.tile_removed_title),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.align(Alignment.TopEnd).size(20.dp),
        )
    }
}

/** Tile name, one line. */
@Composable
private fun TileName(name: String, secondary: Boolean = false) {
    Text(
        name,
        style = if (secondary) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
        color = if (secondary) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** On, Off, Unavailable, a sensor value with unit or a plain state; also used by the picker (M-04). */
@Composable
internal fun stateText(tile: Tile): String {
    if (!tile.available) return stringResource(R.string.tile_unavailable)
    return when (val content = tile.content) {
        is TileContent.Switch -> stringResource(if (content.on == true) R.string.tile_on else R.string.tile_off)
        is TileContent.Sensor -> listOfNotNull(content.value, content.unit).joinToString(" ")
        is TileContent.Climate -> content.target ?: content.current.orEmpty()
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
}

/** Secondary text colour of a tile. */
@Composable
private fun secondaryColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

/** Dash and gap of the removed-entity border, in px. */
private val DashPattern = floatArrayOf(12f, 8f)

/** A dashed 1 dp border with the tile's corner radius (removed-entity tile, concept 15.2). */
private fun Modifier.dashedBorder(color: Color): Modifier = drawBehind {
    val radius = 12.dp.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(DashPattern)),
    )
}
