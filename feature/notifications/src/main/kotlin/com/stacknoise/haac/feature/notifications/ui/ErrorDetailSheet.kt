package com.stacknoise.haac.feature.notifications.ui

import android.content.ClipData
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.HaacSheet
import com.stacknoise.haac.core.common.ui.HaacSideSheet
import com.stacknoise.haac.core.common.ui.LocalWindowWidthClass
import com.stacknoise.haac.core.common.ui.WindowWidthClass
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.MonoFontFamily
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.core.common.ui.theme.haacOutlinedButtonColors
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.feature.notifications.R
import com.stacknoise.haac.feature.notifications.domain.NotificationItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

/**
 * Detail of an error entry (concept 17.4) as a bottom sheet in Compact and a side sheet in Medium; Expanded shows
 * it as [ErrorDetailPane].
 */
@Composable
internal fun ErrorDetailSheet(item: NotificationItem, instanceName: String?, onDismiss: () -> Unit) {
    if (LocalWindowWidthClass.current == WindowWidthClass.Compact) {
        HaacSheet(onDismiss) { ErrorDetailContent(item, instanceName) }
    } else {
        HaacSideSheet(onDismiss) {
            Column(Modifier.verticalScroll(rememberScrollState())) { ErrorDetailContent(item, instanceName) }
        }
    }
}

/** The detail of an error entry in a fixed pane next to the list, with a close button (concept 15.7). */
@Composable
internal fun ErrorDetailPane(item: NotificationItem, instanceName: String?, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
            Icon(painterResource(R.drawable.ic_notifications_close), stringResource(R.string.notifications_close))
        }
        ErrorDetailContent(item, instanceName)
    }
}

/**
 * Code, time, instance, technical description and the HAB code of bridge errors; *Copy details* copies them for
 * support requests.
 */
@Composable
private fun ErrorDetailContent(item: NotificationItem, instanceName: String?) {
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
    Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(stringResource(code.message), style = MaterialTheme.typography.titleMedium)
        rows.forEach { (label, value) -> DetailRow(label, value) }
        OutlinedButton(
            onClick = {
                val details = rows.joinToString("\n") { (label, value) -> "$label: $value" }
                scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(code.code, details))) }
            },
            shape = HaacShapes.Button,
            colors = haacOutlinedButtonColors(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        ) {
            Text(stringResource(R.string.notifications_detail_copy))
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
