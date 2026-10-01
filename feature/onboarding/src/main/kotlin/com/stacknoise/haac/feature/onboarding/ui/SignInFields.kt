package com.stacknoise.haac.feature.onboarding.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.MonoFontFamily
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.common.ui.theme.haacTextFieldColors
import com.stacknoise.haac.feature.onboarding.R

/** Where the install hint links to (concept 4.2 step 4). */
private const val BridgeRepositoryUrl = "https://github.com/stacknoise/haac-bridge"

/** Username and password fields of the mockup; the password never leaves [password] as a String. */
@Composable
internal fun CredentialFields(state: OnboardingUiState, password: TextFieldState, actions: OnboardingActions) {
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = state.username,
        onValueChange = actions.onUsernameChanged,
        placeholder = { Text(stringResource(R.string.onboarding_username)) },
        leadingIcon = { FieldIcon(R.drawable.ic_onboarding_user) },
        singleLine = true,
        enabled = !state.busy,
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
        shape = HaacShapes.Medium,
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Username },
    )
    Spacer(Modifier.height(12.dp))
    OutlinedSecureTextField(
        state = password,
        placeholder = { Text(stringResource(R.string.onboarding_password)) },
        leadingIcon = { FieldIcon(R.drawable.ic_onboarding_lock) },
        enabled = !state.busy,
        shape = HaacShapes.Medium,
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Password },
    )
}

/** MFA step: explanation and code field (concept 5.1). */
@Composable
internal fun CodeSection(state: OnboardingUiState, actions: OnboardingActions) {
    Text(stringResource(R.string.onboarding_code_title).uppercase(), style = SectionLabelStyle)
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.onboarding_code_text), style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = state.code,
        onValueChange = actions.onCodeChanged,
        placeholder = { Text(stringResource(R.string.onboarding_code_hint)) },
        singleLine = true,
        enabled = !state.busy,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        textStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
        shape = HaacShapes.Medium,
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.SmsOtpCode },
    )
}

/** Install hint when HAAC Bridge is missing or outdated (concept 4.2 step 4, 14.1). */
@Composable
internal fun BridgeHint() {
    val uriHandler = LocalUriHandler.current
    Text(stringResource(R.string.onboarding_bridge_text), style = MaterialTheme.typography.bodyMedium)
    TextButton(onClick = { uriHandler.openUri(BridgeRepositoryUrl) }) {
        Text(stringResource(R.string.onboarding_bridge_link), color = MaterialTheme.colorScheme.primary)
    }
}

/** Leading icon of an input field. */
@Composable
private fun FieldIcon(@DrawableRes icon: Int) {
    Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** White fields with a pale outline that turns accent-green on focus. */
@Composable
internal fun fieldColors(): TextFieldColors = haacTextFieldColors()
