package com.stacknoise.haac.core.network.tls

import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.http.HaHttpClient
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PinningTest {
    private val certificate = HeldCertificate.Builder().commonName("localhost").build()
    private val server = MockWebServer()
    private val pins = PinRegistry()
    private val http = HaHttpClient(OkHttpClient.Builder().pinnedBy(pins).build(), DefaultErrorFactory())
    private lateinit var url: HttpUrl

    @BeforeEach
    fun start() {
        server.useHttps(HandshakeCertificates.Builder().heldCertificate(certificate).build().sslSocketFactory())
        server.start()
        server.enqueue(MockResponse.Builder().code(200).body("ok").build())
        url = server.url("/")
    }

    @AfterEach
    fun stop() = server.close()

    private suspend fun failure(): ErrorCode = assertThrows<NetworkException> { http.get(url) }.code

    @Test
    fun `a self-signed certificate is untrusted until it is pinned`() = runTest {
        assertEquals(ErrorCode.NET_CERTIFICATE_UNTRUSTED, failure())
    }

    @Test
    fun `the pinned key is trusted although the system does not know the certificate`() = runTest {
        pins.trust(url, PeerCertificate.keyHashOf(certificate.certificate))
        assertEquals("ok", http.get(url).body)
    }

    @Test
    fun `another key at a pinned address is refused with NET-003`() = runTest {
        pins.trust(url, "00".repeat(32))
        assertEquals(ErrorCode.NET_CERTIFICATE_CHANGED, failure())
    }

    @Test
    fun `the probe reads the certificate without completing the handshake`() = runTest {
        val seen = TlsCertificateProbe().inspect(url)
        assertEquals(PeerCertificate.of(certificate.certificate), seen)
        assertEquals(0, server.requestCount)
    }
}
