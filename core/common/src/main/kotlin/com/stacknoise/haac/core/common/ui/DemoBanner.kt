package com.stacknoise.haac.core.common.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.R
import com.stacknoise.haac.core.common.ui.theme.HaacColors

/** Slim banner under the top bar of the demo: made-up data, no server connected (concept 20.4). */
@Composable
fun DemoBanner(modifier: Modifier = Modifier) {
    Surface(color = HaacColors.AccentTint, modifier = modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.demo_banner),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}
