package com.stacknoise.haac.core.network.auth

import java.nio.CharBuffer

/**
 * Builds the JSON body with the password without ever creating a String of it (concept 5.1).
 * Every intermediate buffer is overwritten and the UTF-8 bytes are written once into an exactly sized array;
 * the caller wipes them after the request. Copies inside OkHttp and Okio cannot be wiped (review S-04).
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

        /** Encodes the content as UTF-8 into an array of exactly the needed size, so no copy is left behind. */
        fun toUtf8(): ByteArray {
            val bytes = ByteArray(utf8Length())
            var out = 0
            var i = 0
            while (i < length) {
                var cp = buffer[i].code
                if (Character.isHighSurrogate(buffer[i]) && i + 1 < length && Character.isLowSurrogate(buffer[i + 1])) {
                    cp = Character.toCodePoint(buffer[i], buffer[i + 1])
                    i++
                } else if (Character.isSurrogate(buffer[i])) {
                    cp = REPLACEMENT
                }
                i++
                out = put(bytes, out, cp)
            }
            return bytes
        }

        /** The number of UTF-8 bytes of the content; a lone surrogate counts as the replacement character. */
        private fun utf8Length(): Int {
            var size = 0
            var i = 0
            while (i < length) {
                val c = buffer[i]
                val paired = Character.isHighSurrogate(c) && i + 1 < length && Character.isLowSurrogate(buffer[i + 1])
                size += when {
                    paired -> 4
                    Character.isSurrogate(c) || c.code >= TWO_BYTE_LIMIT -> 3
                    c.code >= ONE_BYTE_LIMIT -> 2
                    else -> 1
                }
                i += if (paired) 2 else 1
            }
            return size
        }

        /** Writes [cp] as UTF-8 at [at] and returns the next position. */
        private fun put(bytes: ByteArray, at: Int, cp: Int): Int {
            var n = at
            when {
                cp < ONE_BYTE_LIMIT -> bytes[n++] = cp.toByte()
                cp < TWO_BYTE_LIMIT -> {
                    bytes[n++] = (0xC0 or (cp shr 6)).toByte()
                    bytes[n++] = (0x80 or (cp and 0x3F)).toByte()
                }
                cp < FOUR_BYTE_LIMIT -> {
                    bytes[n++] = (0xE0 or (cp shr 12)).toByte()
                    bytes[n++] = (0x80 or ((cp shr 6) and 0x3F)).toByte()
                    bytes[n++] = (0x80 or (cp and 0x3F)).toByte()
                }
                else -> {
                    bytes[n++] = (0xF0 or (cp shr 18)).toByte()
                    bytes[n++] = (0x80 or ((cp shr 12) and 0x3F)).toByte()
                    bytes[n++] = (0x80 or ((cp shr 6) and 0x3F)).toByte()
                    bytes[n++] = (0x80 or (cp and 0x3F)).toByte()
                }
            }
            return n
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
            const val ONE_BYTE_LIMIT = 0x80
            const val TWO_BYTE_LIMIT = 0x800
            const val FOUR_BYTE_LIMIT = 0x10000
            const val REPLACEMENT = 0xFFFD
        }
    }
}
