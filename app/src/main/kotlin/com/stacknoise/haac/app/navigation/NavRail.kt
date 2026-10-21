package com.stacknoise.haac.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacColors

/**
 * Navigation rail of tablets (concept 15.7): the [tabs] above, Settings at the bottom, [currentRoute] selected.
 * Draws an accent bar at the left edge, not the M3 pill, and a border at its right edge.
 */
@Composable
internal fun NavRail(tabs: List<TopLevelDestination>, currentRoute: String?, onOpen: (TopLevelDestination) -> Unit) {
    val (settings, others) = tabs.partition { it == TopLevelDestination.SETTINGS }
    Row(Modifier.fillMaxHeight().background(HaacColors.NavBackground)) {
        Column(
            modifier = Modifier.width(RailWidth).fillMaxHeight().padding(vertical = 12.dp).selectableGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            others.forEach { RailItem(it, selected = currentRoute == it.route) { onOpen(it) } }
            Spacer(Modifier.weight(1f))
            settings.forEach { RailItem(it, selected = currentRoute == it.route) { onOpen(it) } }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(HaacColors.NavBorder))
    }
}

/** One item of the rail: icon over label, with a 3×24 dp accent bar at the left edge when [selected]. */
@Composable
private fun RailItem(destination: TopLevelDestination, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) HaacColors.Accent else HaacColors.OnSurfaceVariant
    Box(
        modifier = Modifier
            .size(width = RailWidth, height = RailItemHeight)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
    ) {
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .size(width = 3.dp, height = 24.dp)
                    .background(color, RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)),
            )
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painterResource(destination.icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp),
            )
            Text(
                stringResource(destination.label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = color,
            )
        }
    }
}

/** Width of the rail (concept 15.7). */
private val RailWidth = 80.dp

/** Height of one rail item. */
private val RailItemHeight = 68.dp
