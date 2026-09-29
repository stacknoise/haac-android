package com.stacknoise.haac.feature.instance.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.feature.instance.R
import com.stacknoise.haac.feature.instance.domain.SwitchStep

/**
 * *Settings > Instances* (concept 4.4): all instances, a tap switches to one, *Add instance* runs the sign-in
 * flow again. [onSwitched] receives the chosen instance and what follows the switch.
 */
@Composable
fun InstancesSection(
    onSwitched: (String, SwitchStep) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InstanceSwitcherViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.instance_section_title).uppercase(), style = SectionLabelStyle)
        items.forEach { item ->
            InstanceRow(
                item = item,
                connection = connection,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !item.active) {
                        viewModel.onSelect(item.id) { step -> onSwitched(item.id, step) }
                    }
                    .padding(vertical = 4.dp),
            )
        }
        OutlinedButton(
            onClick = onAdd,
            shape = HaacShapes.Medium,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(stringResource(R.string.instance_add), style = MaterialTheme.typography.titleSmall)
        }
    }
}
