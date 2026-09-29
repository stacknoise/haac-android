package com.stacknoise.haac.feature.notifications.ui

import android.content.ClipData
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.MonoFontFamily
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.feature.notifications.R
import com.stacknoise.haac.feature.notifications.domain.NotificationItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

/**
 * Detail sheet of an error entry (concept 17.4): code, time, instance, technical description and the HAB code
 * of bridge errors; *Copy details* copies them for support requests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ErrorDetailSheet(item: NotificationItem, instanceName: String?, onDismiss: () -> Unit) {
    val code = item.error ?: ErrorCode.APP_UNEXPECTED
    val time = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .format(Instant.ofEpochMilli(item.createdAt).atZone(ZoneId.systemDefault()))
    val instanceLabel = stringResource(R.string.notifications_detail_instance)
    val rows = listOfNotNull(
        stringResource(R.string.notifications_detail_code) to code.code,
        stringResource(R.string.notifications_detail_time) to time,
        instanceName?.takeIf { item.serverId != null }?.let { instanceLabel to it },
        stringResource(R.string.notifications_detail_description) to code.description,
        item.bridgeCode?.let { stringResource(R.string.notifications_detail_bridge_code) to it },
    )
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(stringResource(code.message), style = MaterialTheme.typography.titleMedium)
            rows.forEach { (label, value) -> DetailRow(label, value) }
            OutlinedButton(
                onClick = {
                    val details = rows.joinToString("\n") { (label, value) -> "$label: $value" }
                    scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(code.code, details))) }
                },
                shape = HaacShapes.Medium,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            ) {
                Text(stringResource(R.string.notifications_detail_copy), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** A section label and its value; codes are shown in the mono font. */
@Composable
private fun DetailRow(label: String, value: String) {
    Text(label.uppercase(), style = SectionLabelStyle, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
    val mono = value.startsWith("HAAC-") || value.startsWith("HAB-")
    Text(
        value,
        style = if (mono) TextStyle(fontFamily = MonoFontFamily) else MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}
