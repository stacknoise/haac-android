package com.stacknoise.haac.core.network.auth

import java.nio.CharBuffer

/**
 * Builds the JSON body with the password without ever creating a String of it (concept 5.1).
 * Every intermediate buffer is overwritten; the caller wipes the returned bytes after the request.
 */
internal object SecretJson {
    /** `{"client_id": …, "username": …, "password": …}` as UTF-8 bytes. */
    fun credentials(clientId: String, username: String, password: CharArray): ByteArray {
        val chars = WipingChars()
        try {
            chars.append("{\"client_id\":")
            chars.quoted(clientId)
            chars.append(",\"username\":")
            chars.quoted(username)
            chars.append(",\"password\":")
            chars.quoted(CharBuffer.wrap(password))
            chars.append("}")
            return chars.toUtf8()
        } finally {
            chars.wipe()
        }
    }

    /** A growable char buffer that zeroes every array it gives up. */
    private class WipingChars {
        private var buffer = CharArray(INITIAL_SIZE)
        private var length = 0

        /** Appends [text] unchanged. */
        fun append(text: CharSequence) = text.forEach(::put)

        /** Appends [text] as a JSON string literal. */
        fun quoted(text: CharSequence) {
            put('"')
            text.forEach(::escaped)
            put('"')
        }

        /** Appends one character with JSON escaping. */
        private fun escaped(c: Char) {
            when {
                c == '"' || c == '\\' -> {
                    put('\\')
                    put(c)
                }
                c < ' ' -> append("\\u" + c.code.toString(HEX).padStart(UNICODE_DIGITS, '0'))
                else -> put(c)
            }
        }

        /** Appends one character, growing the buffer if needed. */
        private fun put(c: Char) {
            if (length == buffer.size) {
                val bigger = buffer.copyOf(buffer.size * 2)
                buffer.fill('\u0000')
                buffer = bigger
            }
            buffer[length++] = c
        }

        /** Encodes the content as UTF-8 and wipes the encoder's buffer. */
        fun toUtf8(): ByteArray {
            val encoded = Charsets.UTF_8.newEncoder().encode(CharBuffer.wrap(buffer, 0, length))
            val bytes = ByteArray(encoded.remaining())
            encoded.get(bytes)
            if (encoded.hasArray()) encoded.array().fill(0)
            return bytes
        }

        /** Overwrites the content. */
        fun wipe() {
            buffer.fill('\u0000')
            length = 0
        }

        /** Buffer and escape sizes. */
        private companion object {
            const val INITIAL_SIZE = 256
            const val HEX = 16
            const val UNICODE_DIGITS = 4
        }
    }
}
