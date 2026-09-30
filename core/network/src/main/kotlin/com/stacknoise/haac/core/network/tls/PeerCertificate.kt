package com.stacknoise.haac.core.network.tls

import java.security.MessageDigest
import java.security.cert.X509Certificate

/**
 * The certificate a server presents (concept 4.3): [keyHash] is the SHA-256 of its public key in lower-case hex
 * (what is pinned), [fingerprint] the SHA-256 of the whole certificate as colon-separated upper-case hex (what
 * browsers show, for the manual comparison), [subject] its owner and [expiresAt] the end of validity in epoch ms.
 */
data class PeerCertificate(val keyHash: String, val fingerprint: String, val subject: String, val expiresAt: Long) {
    /** Companion that reads a certificate. */
    companion object {
        /** The values of [certificate]. */
        fun of(certificate: X509Certificate) = PeerCertificate(
            keyHash = keyHashOf(certificate),
            fingerprint = sha256(certificate.encoded).joinToString(":") { "%02X".format(it) },
            subject = certificate.subjectX500Principal.name,
            expiresAt = certificate.notAfter.time,
        )

        /** SHA-256 of the public key of [certificate] (DER SubjectPublicKeyInfo) as lower-case hex. */
        fun keyHashOf(certificate: X509Certificate): String =
            sha256(certificate.publicKey.encoded).joinToString("") { "%02x".format(it) }

        /** SHA-256 digest of [bytes]. */
        private fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)
    }
}
