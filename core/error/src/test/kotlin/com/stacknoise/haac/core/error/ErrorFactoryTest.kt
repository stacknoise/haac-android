package com.stacknoise.haac.core.error

import java.io.IOException
import java.net.UnknownHostException
import java.security.KeyStoreException
import java.security.ProviderException
import javax.net.ssl.SSLHandshakeException
import kotlin.coroutines.cancellation.CancellationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** Checks the error mapping of concept 17.3 and the HAB -> HAAC table of 18.3. */
class ErrorFactoryTest {
    private val factory = DefaultErrorFactory()

    @Test
    fun `HaacException is returned unchanged`() {
        val error = NetworkException(ErrorCode.NET_CONNECTION_LOST)
        assertSame(error, factory.from(error))
    }

    @Test
    fun `cancellation passes through`() {
        assertThrows<CancellationException> { factory.from(CancellationException("cancelled")) }
    }

    @Test
    fun `io errors become NET-001`() {
        assertEquals(ErrorCode.NET_UNREACHABLE, factory.from(IOException()).code)
        assertEquals(ErrorCode.NET_UNREACHABLE, factory.from(UnknownHostException()).code)
    }

    @Test
    fun `untrusted certificates become NET-007`() {
        assertEquals(ErrorCode.NET_CERTIFICATE_UNTRUSTED, factory.from(SSLHandshakeException("self-signed")).code)
    }

    @Test
    fun `a handshake that failed on the pinned key becomes NET-003`() {
        val failure = SSLHandshakeException("pin").apply { initCause(CertificatePinException()) }
        assertEquals(ErrorCode.NET_CERTIFICATE_CHANGED, factory.from(failure).code)
    }

    @Test
    fun `keystore failures become SEC-002`() {
        assertEquals(ErrorCode.SEC_STORAGE_UNAVAILABLE, factory.from(KeyStoreException("no key")).code)
        assertEquals(ErrorCode.SEC_STORAGE_UNAVAILABLE, factory.from(ProviderException("keystore")).code)
    }

    @Test
    fun `unknown throwables become APP-000 and keep the cause`() {
        val cause = IllegalStateException("boom")
        val error = factory.from(cause)
        assertInstanceOf(UnexpectedException::class.java, error)
        assertSame(cause, error.cause)
    }

    @ParameterizedTest
    @CsvSource(
        "HAB-AUTH-001, HAAC-AUTH-003",
        "HAB-AUTH-002, HAAC-AUTH-006",
        "HAB-SVC-001, HAAC-BRG-003",
        "HAB-SVC-002, HAAC-BRG-004",
        "HAB-SVC-003, HAAC-BRG-005",
        "HAB-ENT-001, HAAC-ENT-001",
        "HAB-HIST-001, HAAC-BRG-006",
        "HAB-HIST-002, HAAC-BRG-006",
        "HAB-WS-001, HAAC-BRG-005",
        "HAB-INT-000, HAAC-BRG-005",
        "HAB-SCH-001, HAAC-SCH-003",
        "HAB-SCH-003, HAAC-SCH-001",
        "HAB-SCH-004, HAAC-SCH-004",
        "HAB-SCH-005, HAAC-SCH-005",
        "HAB-SCH-006, HAAC-SCH-006",
        "HAB-SCH-007, HAAC-BRG-005",
        "HAB-SCH-002, HAAC-BRG-005",
        "HAB-XYZ-999, HAAC-BRG-005",
    )
    fun `bridge codes map to app codes`(hab: String, haac: String) {
        val error = factory.fromBridgeError(hab)
        assertEquals(haac, error.code.code)
        assertEquals(hab, error.bridgeCode)
    }

    @Test
    fun `a deactivated user from the bridge is an auth error`() {
        val error = factory.fromBridgeError("HAB-AUTH-002")
        assertInstanceOf(AuthException::class.java, error)
        assertEquals(ErrorCode.AUTH_USER_BLOCKED, error.code)
    }
}
