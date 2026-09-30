package com.stacknoise.haac.core.network.tls

import com.stacknoise.haac.core.error.CertificatePinException
import android.annotation.SuppressLint
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager

/**
 * Trust decision of every TLS connection (concept 4.3): an address with a pin is trusted exactly when the server's
 * public key is the pinned one, whatever the system thinks of its certificate; any other address needs a
 * certificate the system trusts. A different key at a pinned address fails with [CertificatePinException].
 */
@SuppressLint("CustomX509TrustManager") // delegates to the system trust manager for every address that has no pin
class PinningTrustManager(
    private val system: X509TrustManager,
    private val pins: PinRegistry,
) : X509ExtendedTrustManager() {
    /** Client certificates are never used. */
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
        system.checkClientTrusted(chain, authType)

    /** Client certificates are never used. */
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket) =
        system.checkClientTrusted(chain, authType)

    /** Client certificates are never used. */
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine) =
        system.checkClientTrusted(chain, authType)

    /** Without a peer the pin cannot be looked up: the system decides. */
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) =
        system.checkServerTrusted(chain, authType)

    /** Decides for the address of [socket]'s handshake. */
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket) {
        val session = (socket as? SSLSocket)?.handshakeSession
        val pin = session?.let { pins.pinFor(it.peerHost, it.peerPort) }
        if (pin == null) systemCheck(chain, authType, socket) else requirePin(chain, pin)
    }

    /** Decides for the address of [engine]'s handshake. */
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine) {
        val session = engine.handshakeSession
        val pin = session?.let { pins.pinFor(it.peerHost, it.peerPort) }
        if (pin == null) system.checkServerTrusted(chain, authType) else requirePin(chain, pin)
    }

    /** The system's accepted issuers. */
    override fun getAcceptedIssuers(): Array<X509Certificate> = system.acceptedIssuers

    /** System check with the socket, so the platform can do its own endpoint checks. */
    private fun systemCheck(chain: Array<X509Certificate>, authType: String, socket: Socket) {
        if (system is X509ExtendedTrustManager) {
            system.checkServerTrusted(chain, authType, socket)
        } else {
            system.checkServerTrusted(chain, authType)
        }
    }

    /** Accepts the chain if its first certificate carries the pinned key. */
    private fun requirePin(chain: Array<X509Certificate>, pin: String) {
        if (chain.isEmpty() || PeerCertificate.keyHashOf(chain[0]) != pin) throw CertificatePinException()
    }
}
