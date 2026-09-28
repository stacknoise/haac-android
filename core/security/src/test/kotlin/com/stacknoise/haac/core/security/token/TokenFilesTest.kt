package com.stacknoise.haac.core.security.token

import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

class TokenFilesTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `fingerprint format keeps generation, window, iv and ciphertext`() {
        val files = TokenFiles(dir)
        files.write("a", SealedToken(TokenProtection.Fingerprint(7, 300), byteArrayOf(1, 2, 3), byteArrayOf(9, 8)))
        val read = files.read("a")!!
        assertEquals(TokenProtection.Fingerprint(7, 300), read.protection)
        assertArrayEquals(byteArrayOf(1, 2, 3), read.iv)
        assertArrayEquals(byteArrayOf(9, 8), read.ciphertext)
        assertFalse(File(dir, "a.tmp").exists())
    }

    @Test
    fun `files of format 1 from part 2 are still read`() {
        DataOutputStream(File(dir, "a.bin").outputStream()).use { out ->
            out.writeByte(1)
            out.writeByte(2)
            out.write(byteArrayOf(4, 5))
            out.write(byteArrayOf(6))
        }
        val read = TokenFiles(dir).read("a")!!
        assertEquals(TokenProtection.DeviceKey, read.protection)
        assertArrayEquals(byteArrayOf(4, 5), read.iv)
        assertArrayEquals(byteArrayOf(6), read.ciphertext)
    }

    @Test
    fun `missing file is null and an unknown format fails`() {
        val files = TokenFiles(dir)
        assertNull(files.read("a"))
        File(dir, "a.bin").writeBytes(byteArrayOf(9))
        assertThrows<IOException> { files.read("a") }
    }
}
