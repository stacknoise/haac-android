package com.stacknoise.haac.feature.entities.guard

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.feature.entities.R

/** The small shield of a guarded tile (concept 8.5, M-27, M-28); also the reminder in the schedule editor. */
@Composable
fun GuardShield(modifier: Modifier = Modifier) {
    Icon(
        painterResource(R.drawable.ic_entities_shield),
        contentDescription = stringResource(R.string.guard_shield),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(end = 8.dp).size(18.dp),
    )
}
