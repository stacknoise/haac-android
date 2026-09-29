package com.stacknoise.haac.core.network.tls

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import android.annotation.SuppressLint
import java.net.InetSocketAddress
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl

/** Reads the certificate a server presents, so the user can compare and trust it (concept 4.3). */
fun interface CertificateProbe {
    /** The certificate of the `https` address [url]; HAAC-NET-001 if the server does not answer with one. */
    suspend fun inspect(url: HttpUrl): PeerCertificate
}

/**
 * [CertificateProbe] with a bare TLS handshake. The probe's trust manager notes the chain and then refuses it, so
 * the handshake never completes and no application data, credentials or tokens are ever sent to the server.
 */
class TlsCertificateProbe @Inject constructor() : CertificateProbe {
    /** Connects, lets the handshake fail on purpose and returns what the server presented. */
    override suspend fun inspect(url: HttpUrl): PeerCertificate = withContext(Dispatchers.IO) {
        val seen = AtomicReference<X509Certificate?>()
        val context = SSLContext.getInstance("TLS").apply { init(null, arrayOf(Recorder(seen)), null) }
        try {
            (context.socketFactory.createSocket() as SSLSocket).use { socket ->
                socket.soTimeout = TIMEOUT_MS
                socket.connect(InetSocketAddress(url.host, url.port), TIMEOUT_MS)
                socket.sslParameters = socket.sslParameters.apply {
                    if (url.isName()) serverNames = listOf(SNIHostName(url.host))
                }
                socket.startHandshake()
            }
        } catch (_: java.io.IOException) {
            // The handshake is meant to fail; what matters is the recorded certificate.
        }
        seen.get()?.let(PeerCertificate::of) ?: throw NetworkException(ErrorCode.NET_UNREACHABLE)
    }

    /** Notes the server's certificate and refuses it. */
    @SuppressLint("CustomX509TrustManager") // refuses every chain, so no connection is ever established
    private class Recorder(private val seen: AtomicReference<X509Certificate?>) : X509TrustManager {
        /** Never called: the app is the client. */
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = throw CertificateException()

        /** Notes the first certificate and refuses the chain. */
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            seen.set(chain.firstOrNull())
            throw CertificateException("Inspection only")
        }

        /** No issuers are accepted. */
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    /** Connect and read timeout. */
    private companion object {
        const val TIMEOUT_MS = 5_000
    }
}

/** True if the host is a DNS name; only names are sent as SNI, not IP addresses. */
private fun HttpUrl.isName(): Boolean = ':' !in host && host.any { it.isLetter() }
