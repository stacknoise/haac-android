package com.stacknoise.haac.feature.instance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.network.connection.ConnectionState
import com.stacknoise.haac.feature.instance.R
import com.stacknoise.haac.feature.instance.domain.InstanceItem

/** Name with the accent colour, the address and the connection status of one instance (concept 4.4). */
@Composable
fun InstanceRow(item: InstanceItem, connection: ConnectionState, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(item.accent))
                .border(1.dp, HaacColors.OutlineStrong, CircleShape),
        )
        Column {
            Text(item.label(), style = MaterialTheme.typography.titleMedium)
            Text(
                listOf(item.address, statusText(item, connection)).filter { it.isNotEmpty() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The name, followed by the HA user when the same server is stored for several users. */
@Composable
private fun InstanceItem.label(): String =
    if (userName == null) name else stringResource(R.string.instance_user, name, userName)

/** Status line: only the active instance holds a connection (concept 4.4). */
@Composable
private fun statusText(item: InstanceItem, connection: ConnectionState): String = stringResource(
    when {
        !item.active -> R.string.instance_status_inactive
        connection is ConnectionState.Connected -> R.string.instance_status_connected
        connection is ConnectionState.Reconnecting -> R.string.instance_status_reconnecting
        connection is ConnectionState.Failed -> R.string.instance_status_failed
        else -> R.string.instance_status_connecting
    },
)
