package com.stacknoise.haac.feature.notifications.ui

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.feature.notifications.R

/** The bell that opens the notification list, with an accent dot while entries are unread (M-08, M-09). */
@Composable
fun NotificationBell(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationBellViewModel = hiltViewModel(),
) {
    val unread by viewModel.unread.collectAsStateWithLifecycle()
    IconButton(onClick = onClick, modifier = modifier) {
        BadgedBox(badge = { if (unread) Badge(containerColor = MaterialTheme.colorScheme.primary) }) {
            Icon(painterResource(R.drawable.ic_notifications_bell), stringResource(R.string.notifications_title))
        }
    }
}
