package com.stacknoise.haac.feature.entities.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.feature.entities.R
import com.stacknoise.haac.feature.entities.domain.EntityControl
import com.stacknoise.haac.feature.entities.domain.Tile
import com.stacknoise.haac.feature.entities.domain.TileContent

/**
 * The climate tile (M-05, concept 8.4): name, heating mark, target temperature on an arc from `min_temp` to
 * `max_temp`, the current temperature below it, and − / + for the target.
 */
@Composable
internal fun ClimateTile(tile: Tile, climate: TileContent.Climate, enabled: Boolean, actions: TileActions) {
    val control = tile.control as? EntityControl.TargetTemperature
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                tile.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (climate.heating && tile.available) {
                Icon(
                    painterResource(R.drawable.ic_entities_heating),
                    stringResource(R.string.tile_heating),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
        ) {
            TemperatureArc(control?.fraction, Modifier.fillMaxHeight().aspectRatio(1f))
            ArcCenter(tile, climate)
        }
        val lower = enabled && control?.stepped(up = false) != null
        val raise = enabled && control?.stepped(up = true) != null
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StepButton(R.drawable.ic_entities_minus, R.string.tile_colder, lower) { actions.onStep(tile, false) }
            StepButton(R.drawable.ic_entities_add, R.string.tile_warmer, raise) { actions.onStep(tile, true) }
        }
    }
}

/** Target temperature large and "now 20.8°" below, or *Unavailable*. */
@Composable
private fun ArcCenter(tile: Tile, climate: TileContent.Climate) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (!tile.available) {
            Text(stringResource(R.string.tile_unavailable), style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        Text(climate.target ?: "–", style = MaterialTheme.typography.headlineLarge, maxLines = 1)
        climate.current?.let {
            Text(
                stringResource(R.string.tile_now, it),
                style = MaterialTheme.typography.bodySmall,
                color = secondaryColor(),
                maxLines = 1,
            )
        }
    }
}

/** A 270° arc open at the bottom; the accent part shows [fraction], the track alone when it is null. */
@Composable
private fun TemperatureArc(fraction: Float?, modifier: Modifier) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val width = ArcStroke.toPx()
        val side = size.minDimension - width
        val topLeft = Offset((size.width - side) / 2, (size.height - side) / 2)
        val style = Stroke(width = width, cap = StrokeCap.Round)
        val arc = Size(side, side)
        drawArc(track, ArcStart, ArcSweep, useCenter = false, topLeft = topLeft, size = arc, style = style)
        if (fraction != null && fraction > 0f) {
            val sweep = ArcSweep * fraction
            drawArc(accent, ArcStart, sweep, useCenter = false, topLeft = topLeft, size = arc, style = style)
        }
    }
}

/** An outlined round − or + button. */
@Composable
private fun StepButton(@DrawableRes icon: Int, @StringRes label: Int, enabled: Boolean, onClick: () -> Unit) {
    OutlinedIconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
        Icon(painterResource(icon), stringResource(label), modifier = Modifier.size(20.dp))
    }
}

/** Start angle (lower left) and length of the arc, in degrees. */
private const val ArcStart = 135f
private const val ArcSweep = 270f

/** Width of the arc. */
private val ArcStroke = 6.dp
