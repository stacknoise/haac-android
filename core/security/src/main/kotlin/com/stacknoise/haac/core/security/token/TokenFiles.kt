package com.stacknoise.haac.core.security.token

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException

/** Which key protects a stored refresh token (concept 5.3, 5.4). */
sealed interface TokenProtection {
    /** The device-bound key: usable whenever the device is unlocked. */
    data object DeviceKey : TokenProtection

    /** Fingerprint key [generation], created with [unlockWindowSeconds] (0: one fingerprint per use). */
    data class Fingerprint(val generation: Int, val unlockWindowSeconds: Int) : TokenProtection
}

/** Contents of a token file: protection, IV and AES-GCM ciphertext. */
class SealedToken(val protection: TokenProtection, val iv: ByteArray, val ciphertext: ByteArray)

/**
 * One file per instance under `noBackupFilesDir` (concept 5.3). Format 1: format byte, IV length, IV,
 * ciphertext. Format 2 (fingerprint) adds key generation and unlock window after the format byte.
 */
class TokenFiles(private val directory: File) {
    /** Reads the file of [serverId], or null if there is none. */
    fun read(serverId: String): SealedToken? {
        val file = file(serverId)
        if (!file.exists()) return null
        return DataInputStream(file.inputStream()).use { input ->
            val protection = when (input.readUnsignedByte()) {
                FORMAT_DEVICE_KEY -> TokenProtection.DeviceKey
                FORMAT_FINGERPRINT -> TokenProtection.Fingerprint(input.readInt(), input.readInt())
                else -> throw IOException("unknown token file format")
            }
            val iv = ByteArray(input.readUnsignedByte()).also(input::readFully)
            SealedToken(protection, iv, input.readBytes())
        }
    }

    /** Writes [token] for [serverId] and replaces the old file atomically. */
    fun write(serverId: String, token: SealedToken) {
        directory.mkdirs()
        val temp = File(directory, "$serverId.tmp")
        DataOutputStream(temp.outputStream()).use { out ->
            when (val protection = token.protection) {
                TokenProtection.DeviceKey -> out.writeByte(FORMAT_DEVICE_KEY)
                is TokenProtection.Fingerprint -> {
                    out.writeByte(FORMAT_FINGERPRINT)
                    out.writeInt(protection.generation)
                    out.writeInt(protection.unlockWindowSeconds)
                }
            }
            out.writeByte(token.iv.size)
            out.write(token.iv)
            out.write(token.ciphertext)
        }
        if (!temp.renameTo(file(serverId))) throw IOException("rename failed")
    }

    /** True if a file exists for [serverId]. */
    fun exists(serverId: String): Boolean = file(serverId).exists()

    /** Deletes the file of [serverId]; a missing file is fine. */
    fun delete(serverId: String) {
        file(serverId).delete()
    }

    /** Token file of [serverId]. */
    private fun file(serverId: String) = File(directory, "$serverId.bin")

    /** File format versions. */
    private companion object {
        const val FORMAT_DEVICE_KEY = 1
        const val FORMAT_FINGERPRINT = 2
    }
}
