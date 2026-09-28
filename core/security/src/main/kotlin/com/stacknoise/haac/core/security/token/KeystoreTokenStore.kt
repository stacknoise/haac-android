package com.stacknoise.haac.core.security.token

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.KeystoreException
import com.stacknoise.haac.core.security.keystore.CipherFactory
import com.stacknoise.haac.core.security.keystore.KeyFactory
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One file per instance under `noBackupFilesDir` with format byte, IV and AES-GCM ciphertext (concept 5.3).
 *
 * The key never leaves the Keystore; the file alone is useless on another device.
 */
class KeystoreTokenStore(
    private val directory: File,
    private val keys: KeyFactory,
    private val ciphers: CipherFactory,
    private val errors: ErrorFactory,
) : TokenStore {
    /** Encrypts with the instance key and replaces the file atomically. */
    override suspend fun save(serverId: String, refreshToken: String) = guarded {
        val cipher = ciphers.encrypt(keys.tokenKey(serverId))
        val plain = refreshToken.encodeToByteArray()
        val sealed = try {
            cipher.doFinal(plain)
        } finally {
            plain.fill(0)
        }
        directory.mkdirs()
        val temp = File(directory, "$serverId.tmp")
        DataOutputStream(temp.outputStream()).use { out ->
            out.writeByte(FORMAT)
            out.writeByte(cipher.iv.size)
            out.write(cipher.iv)
            out.write(sealed)
        }
        if (!temp.renameTo(file(serverId))) throw IOException("rename failed")
    }

    /** Reads and decrypts the file of [serverId]. */
    override suspend fun read(serverId: String): String? = guarded {
        val file = file(serverId)
        if (!file.exists()) return@guarded null
        DataInputStream(file.inputStream()).use { input ->
            if (input.readUnsignedByte() != FORMAT) throw IOException("unknown token file format")
            val iv = ByteArray(input.readUnsignedByte()).also(input::readFully)
            val sealed = input.readBytes()
            val plain = ciphers.decrypt(keys.tokenKey(serverId), iv).doFinal(sealed)
            try {
                plain.decodeToString()
            } finally {
                plain.fill(0)
            }
        }
    }

    /** Checks only that the file exists. */
    override suspend fun contains(serverId: String): Boolean = guarded { file(serverId).exists() }

    /** Removes file and key; missing ones are fine. */
    override suspend fun delete(serverId: String) = guarded {
        file(serverId).delete()
        keys.deleteKeys(serverId)
    }

    /** Token file of [serverId]. */
    private fun file(serverId: String) = File(directory, "$serverId.bin")

    /** Runs [block] on the IO dispatcher and converts file and Keystore errors to HAAC-SEC codes. */
    private suspend fun <T> guarded(block: () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: IOException) {
            throw KeystoreException(ErrorCode.SEC_STORAGE_UNAVAILABLE, e)
        } catch (e: GeneralSecurityException) {
            throw errors.from(e)
        } catch (e: ProviderException) {
            throw errors.from(e)
        }
    }

    /** File format version. */
    private companion object {
        const val FORMAT = 1
    }
}
