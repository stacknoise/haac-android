package com.stacknoise.haac.core.common.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.R
import com.stacknoise.haac.core.common.ui.theme.MonoFontFamily
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import java.text.DateFormat
import java.util.Date

/** The certificate a dialog asks about: [expiresAt] is in epoch milliseconds (concept 4.3). */
data class CertificateDetails(val host: String, val fingerprint: String, val subject: String, val expiresAt: Long)

/** What the certificate dialog asks (concept 4.3). */
enum class CertificateDialogKind {
    /** An unknown certificate: trust it and pin its key on first use. */
    TRUST_NEW,

    /** The key differs from the pinned one: trust the new one or keep the connection blocked. */
    TRUST_CHANGED,

    /** The pinned certificate of an address: information, with the option to remove the pin. */
    PINNED,
}

/** Shows [details] for the manual comparison; [onConfirm] is the action of [kind], [onDismiss] closes. */
@Composable
fun CertificateDialog(
    details: CertificateDetails,
    kind: CertificateDialogKind,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(kind.title())) },
        text = {
            Column {
                Text(stringResource(kind.text(), details.host), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Labelled(R.string.certificate_fingerprint, details.fingerprint, mono = true)
                Labelled(R.string.certificate_subject, details.subject)
                Labelled(R.string.certificate_expires, DateFormat.getDateInstance().format(Date(details.expiresAt)))
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(kind.confirm())) } },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                val closed = kind == CertificateDialogKind.PINNED
                val label = if (closed) R.string.certificate_close else R.string.certificate_cancel
                Text(stringResource(label))
            }
        },
    )
}

/** A small label above its value. */
@Composable
private fun Labelled(label: Int, value: String, mono: Boolean = false) {
    Text(stringResource(label).uppercase(), style = SectionLabelStyle)
    val style = MaterialTheme.typography.bodySmall
    Text(value, style = if (mono) style.copy(fontFamily = MonoFontFamily) else style)
    Spacer(Modifier.height(8.dp))
}

/** Dialog title of the kind. */
private fun CertificateDialogKind.title(): Int = when (this) {
    CertificateDialogKind.TRUST_NEW -> R.string.certificate_title_new
    CertificateDialogKind.TRUST_CHANGED -> R.string.certificate_title_changed
    CertificateDialogKind.PINNED -> R.string.certificate_title_pinned
}

/** Explanation of the kind; its argument is the host. */
private fun CertificateDialogKind.text(): Int = when (this) {
    CertificateDialogKind.TRUST_NEW -> R.string.certificate_text_new
    CertificateDialogKind.TRUST_CHANGED -> R.string.certificate_text_changed
    CertificateDialogKind.PINNED -> R.string.certificate_text_pinned
}

/** Label of the confirming button of the kind. */
private fun CertificateDialogKind.confirm(): Int = when (this) {
    CertificateDialogKind.TRUST_NEW -> R.string.certificate_trust
    CertificateDialogKind.TRUST_CHANGED -> R.string.certificate_trust_new
    CertificateDialogKind.PINNED -> R.string.certificate_remove_pin
}
