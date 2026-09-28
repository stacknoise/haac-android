package com.stacknoise.haac.core.common.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.MonoFontFamily
import com.stacknoise.haac.core.error.ErrorCode

/** User text of an error code plus the code in the mono font (concept 17.4). */
@Composable
fun ErrorMessage(code: ErrorCode, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 16.dp)) {
        Text(
            stringResource(code.message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            code.code,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
