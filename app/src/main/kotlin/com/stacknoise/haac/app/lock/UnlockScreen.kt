package com.stacknoise.haac.app.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stacknoise.haac.R
import com.stacknoise.haac.core.common.ui.ErrorMessage
import com.stacknoise.haac.core.common.ui.SecureWindow
import com.stacknoise.haac.core.common.ui.findActivity
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.HaacTheme
import com.stacknoise.haac.core.common.ui.theme.haacButtonColors
import com.stacknoise.haac.core.error.ErrorCode

/**
 * Unlock screen (concept 5.4, 15.4): the fingerprint prompt opens right away; *Unlock* shows it again.
 * [onUnlocked] opens the main area, [onSignIn] the HA login of this instance.
 */
@Composable
fun UnlockScreen(onUnlocked: () -> Unit, onSignIn: () -> Unit, viewModel: UnlockViewModel = hiltViewModel()) {
    SecureWindow()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity() as? FragmentActivity
    val unlocked by rememberUpdatedState(onUnlocked)
    val signIn by rememberUpdatedState(onSignIn)
    LaunchedEffect(activity) {
        activity?.let(viewModel::onUnlock)
    }
    LaunchedEffect(state.result) {
        when (state.result) {
            UnlockResult.UNLOCKED -> unlocked()
            UnlockResult.SIGN_IN -> signIn()
            null -> Unit
        }
    }
    UnlockContent(
        state = state,
        onUnlock = { activity?.let(viewModel::onUnlock) },
        onUsePassword = viewModel::onUsePassword,
    )
}

/** Stateless layout: icon, instance, *Unlock* and *Use password*; after HAAC-SEC-001 only the password way. */
@Composable
fun UnlockContent(state: UnlockUiState, onUnlock: () -> Unit, onUsePassword: () -> Unit) {
    val keyGone = state.error == ErrorCode.SEC_BIOMETRICS_CHANGED
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_fingerprint),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.unlock_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            state.instanceName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        state.error?.let { ErrorMessage(it) }
        Spacer(Modifier.height(32.dp))
        if (!keyGone) {
            Button(
                onClick = onUnlock,
                enabled = !state.busy,
                shape = HaacShapes.Button,
                colors = haacButtonColors(),
                modifier = Modifier.fillMaxWidth().height(58.dp),
            ) {
                if (state.busy) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.unlock_action), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        TextButton(onClick = onUsePassword, enabled = !state.busy) {
            Text(stringResource(R.string.unlock_use_password))
        }
    }
}

/** Preview of the locked state. */
@Preview
@Composable
private fun UnlockPreview() {
    HaacTheme {
        UnlockContent(UnlockUiState(instanceName = "Home"), onUnlock = {}, onUsePassword = {})
    }
}
