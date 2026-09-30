package com.stacknoise.haac.core.error

import java.security.cert.CertificateException

/**
 * Thrown inside a TLS handshake when the server's public key is not the key pinned for its address (concept 4.3).
 * [DefaultErrorFactory] turns a handshake failure with this cause into HAAC-NET-003.
 */
class CertificatePinException : CertificateException("The server's key does not match the pinned key")
