package com.stacknoise.haac.core.database.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Non-sensitive app settings in the typed DataStore (concept 12); never secrets. [shortcutKey] only tells the
 * shortcut intents of this installation from foreign ones; it opens no account. [deviceName] is the name the app sends with switching actions: null until the user changes
 * it (the device model is used then), empty for none (concept 21.5). Whether an instance uses
 * fingerprint unlock is not stored here: it follows from the key that protects its token (concept 5.4).
 */
@Serializable
data class AppSettings(
    val activeServerId: String? = null,
    val unlockWindowMinutes: Int = 0,
    val lockTimeoutMinutes: Int = SecuritySettings.DEFAULT_LOCK_TIMEOUT_MINUTES,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val shortcutKey: String? = null,
    val deviceName: String? = null,
)

/** Reads and writes [AppSettings] as JSON. */
object AppSettingsSerializer : Serializer<AppSettings> {
    private val json = Json { ignoreUnknownKeys = true }

    /** Settings before the first write. */
    override val defaultValue: AppSettings = AppSettings()

    /** A broken file is reported to DataStore, which then starts from the defaults. */
    override suspend fun readFrom(input: InputStream): AppSettings = try {
        json.decodeFromString(AppSettings.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("app settings unreadable", e)
    }

    /** Writes the settings as UTF-8 JSON. */
    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        output.write(json.encodeToString(AppSettings.serializer(), t).encodeToByteArray())
    }
}
