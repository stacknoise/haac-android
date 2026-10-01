package com.stacknoise.haac.feature.notifications.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.feature.notifications.R
import com.stacknoise.haac.feature.notifications.domain.NotificationItem

/**
 * Deletes [item] when the row is swiped to the left; screen readers get the same as a custom action
 * (concept 9.1). [content] is the row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeToDelete(
    item: NotificationItem,
    onDelete: (NotificationItem) -> Unit,
    content: @Composable () -> Unit,
) {
    val label = stringResource(R.string.notifications_delete)
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) onDelete(item)
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        modifier = Modifier.clip(HaacShapes.Card).semantics {
            customActions = listOf(
                CustomAccessibilityAction(label) {
                    onDelete(item)
                    true
                },
            )
        },
        backgroundContent = {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 24.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_notifications_delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        Box(
            Modifier
                .background(MaterialTheme.colorScheme.surface, HaacShapes.Card)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, HaacShapes.Card),
        ) { content() }
    }
}
