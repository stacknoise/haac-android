package com.stacknoise.haac.feature.layout.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.feature.layout.R
import com.stacknoise.haac.feature.layout.domain.PlaceKind

/**
 * Floating action button with the create menu *New home / New level / New room* and *Import from Home
 * Assistant* (M-02, 6.3). Without a home only
 * *New home* is offered (concept 6.1: a home is mandatory).
 */
@Composable
fun CreateMenu(hasHome: Boolean, onCreate: (PlaceKind) -> Unit, onImport: () -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (open) {
            MenuEntry(R.string.places_import, R.drawable.ic_layout_import) {
                open = false
                onImport()
            }
            MenuEntry(R.string.places_new_home, R.drawable.ic_layout_home) {
                open = false
                onCreate(PlaceKind.HOME)
            }
            if (hasHome) {
                MenuEntry(R.string.places_new_level, R.drawable.ic_layout_level) {
                    open = false
                    onCreate(PlaceKind.FLOOR)
                }
                MenuEntry(R.string.places_new_room, R.drawable.ic_layout_room) {
                    open = false
                    onCreate(PlaceKind.ROOM)
                }
            }
        }
        FloatingActionButton(
            onClick = { open = !open },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            val (icon, label) = if (open) {
                R.drawable.ic_layout_close to R.string.places_close_menu
            } else {
                R.drawable.ic_layout_add to R.string.places_create
            }
            Icon(painterResource(icon), stringResource(label))
        }
    }
}

/** One entry of the create menu: label and a small outlined button. */
@Composable
private fun MenuEntry(@StringRes label: Int, @DrawableRes icon: Int, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 16.dp),
        )
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            Icon(painterResource(icon), contentDescription = null)
        }
    }
}
