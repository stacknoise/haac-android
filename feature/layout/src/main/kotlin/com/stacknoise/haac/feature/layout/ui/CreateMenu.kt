package com.stacknoise.haac.feature.layout.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacColors
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.feature.layout.R
import com.stacknoise.haac.feature.layout.domain.PlaceKind

/**
 * Floating action button with the create menu *New home / New level / New room* and *Import from Home
 * Assistant* (M-02, 6.3). Without a home only *New home* is offered (concept 6.1: a home is mandatory). The caller
 * owns [open] so it can dim the content behind the open menu.
 */
@Composable
fun CreateMenu(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    hasHome: Boolean,
    onCreate: (PlaceKind) -> Unit,
    onImport: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (open) {
            MenuEntry(R.string.places_import, R.drawable.ic_layout_import) {
                onOpenChange(false)
                onImport()
            }
            MenuEntry(R.string.places_new_home, R.drawable.ic_layout_home) {
                onOpenChange(false)
                onCreate(PlaceKind.HOME)
            }
            if (hasHome) {
                MenuEntry(R.string.places_new_level, R.drawable.ic_layout_level) {
                    onOpenChange(false)
                    onCreate(PlaceKind.FLOOR)
                }
                MenuEntry(R.string.places_new_room, R.drawable.ic_layout_room) {
                    onOpenChange(false)
                    onCreate(PlaceKind.ROOM)
                }
            }
        }
        FloatingActionButton(
            onClick = { onOpenChange(!open) },
            shape = HaacShapes.Button,
            containerColor = HaacColors.Accent,
            contentColor = HaacColors.OnAccent,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            modifier = Modifier
                .shadow(
                    12.dp,
                    HaacShapes.Button,
                    ambientColor = HaacColors.AccentBorder,
                    spotColor = HaacColors.AccentBorder,
                )
                .size(60.dp),
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

/** One entry of the create menu: a white label pill and a 48 dp icon square in the accent tint. */
@Composable
private fun MenuEntry(@StringRes label: Int, @DrawableRes icon: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(HaacShapes.Medium).clickable(role = Role.Button, onClick = onClick),
    ) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .background(HaacColors.Surface, HaacShapes.Small)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, HaacShapes.Small)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(start = 12.dp)
                .size(48.dp)
                .background(HaacColors.AccentTintStrong, HaacShapes.Medium),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = HaacColors.Accent)
        }
    }
}
