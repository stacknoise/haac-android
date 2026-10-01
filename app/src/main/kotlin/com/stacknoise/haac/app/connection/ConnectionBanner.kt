package com.stacknoise.haac.app.connection

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.error.ErrorAction
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.connection.ConnectionState

/**
 * Banner above the main area while the connection is lost or failed (concept 14.1): the error with its code
 * and one action. Nothing is shown while connecting or connected, so returning to the app does not flicker.
 */
@Composable
fun ConnectionBanner(state: ConnectionState, onRetry: () -> Unit, onOpenSettings: () -> Unit) {
    val error: HaacException = when (state) {
        is ConnectionState.Reconnecting -> state.error
        is ConnectionState.Failed -> state.error
        else -> return
    }
    // While reconnecting, "Try again" skips the back-off; a failed connection offers its error's action.
    val action = if (state is ConnectionState.Reconnecting) ErrorAction.RETRY else error.code.action
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.statusBarsPadding().padding(start = 16.dp, end = 8.dp, bottom = 12.dp),
        ) {
            ErrorMessage(error.code, modifier = Modifier.weight(1f))
            val label = action.label
            if (label != null && action != ErrorAction.SIGN_IN) {
                TextButton(onClick = if (action == ErrorAction.OPEN_SETTINGS) onOpenSettings else onRetry) {
                    Text(stringResource(label))
                }
            }
        }
    }
}
