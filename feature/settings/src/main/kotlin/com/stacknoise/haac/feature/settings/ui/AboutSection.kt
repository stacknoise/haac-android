package com.stacknoise.haac.feature.settings.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stacknoise.haac.core.common.ui.theme.HaacShapes
import com.stacknoise.haac.core.common.ui.theme.SectionLabelStyle
import com.stacknoise.haac.feature.settings.R

/** *Settings → About*: app version, the app's own license and the entry to the library licenses (concept 16.2). */
@Composable
fun AboutSection(onOpenLicenses: () -> Unit) {
    val version = LocalContext.current.versionName()
    Text(stringResource(R.string.settings_about).uppercase(), style = SectionLabelStyle)
    Spacer(Modifier.height(8.dp))
    version?.let {
        Text(stringResource(R.string.about_version, it), style = MaterialTheme.typography.bodyMedium)
    }
    Text(
        stringResource(R.string.about_license),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = onOpenLicenses,
        shape = HaacShapes.Medium,
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) {
        Text(stringResource(R.string.about_open_licenses), style = MaterialTheme.typography.titleMedium)
    }
}

/** The version name of the installed app, or null when the package cannot be read. */
private fun Context.versionName(): String? = try {
    packageManager.getPackageInfo(packageName, 0).versionName
} catch (_: PackageManager.NameNotFoundException) {
    null
}
