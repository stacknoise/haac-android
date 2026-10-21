package com.stacknoise.haac.feature.entities.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.feature.entities.R

/** "Offline: showing the last known states" while the connection has been down for a while (concept 14.1). */
@Composable
internal fun StaleBanner() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(MaterialTheme.colorScheme.surface, HaacShapes.Card)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, HaacShapes.Card)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(painterResource(R.drawable.ic_entities_warning), contentDescription = null, Modifier.size(20.dp))
        Text(
            stringResource(R.string.rooms_stale),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** "1 entity no longer exists in Home Assistant" with *Review* (M-08). */
@Composable
internal fun WithdrawnBanner(count: Int, onReview: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(MaterialTheme.colorScheme.surface, HaacShapes.Card)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, HaacShapes.Card)
            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_entities_warning),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Text(
            pluralStringResource(R.plurals.rooms_withdrawn_banner, count, count),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        TextButton(onClick = onReview) { Text(stringResource(R.string.rooms_review)) }
    }
}
