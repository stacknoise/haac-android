package com.stacknoise.haac.app.access

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.stacknoise.haac.core.network.access.LocalNetworkAccess

/**
 * Asks once for `ACCESS_LOCAL_NETWORK` while [enabled] and the permission is missing (concept 4.2, 4.5); the
 * system remembers a refusal. [onResult] receives the answer, e.g. to try the connection again.
 */
@Composable
fun RequestLocalNetworkAccess(
    enabled: Boolean = true,
    onResult: (Boolean) -> Unit = {},
    viewModel: LocalNetworkViewModel = hiltViewModel(),
) {
    val result by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result(it) }
    LaunchedEffect(enabled) {
        if (enabled && !viewModel.granted()) launcher.launch(LocalNetworkAccess.PERMISSION)
    }
}
