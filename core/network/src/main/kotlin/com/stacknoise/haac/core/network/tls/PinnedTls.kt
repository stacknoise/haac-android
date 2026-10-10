package com.stacknoise.haac.core.network.tls

import java.security.KeyStore
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSessionContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient

/** The system's default trust manager. */
internal fun systemTrustManager(): X509TrustManager {
    val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    factory.init(null as KeyStore?)
    return factory.trustManagers.filterIsInstance<X509TrustManager>().first()
}

/**
 * Makes this client trust by [pins] (concept 4.3): pinned addresses by their key, all others by the system's
 * certificates. The host name is checked as usual, except for a pinned address, where the pin identifies the server.
 * When a pin changes, idle connections and cached TLS sessions are dropped, so the next request is checked against
 * the new pins instead of resuming a connection or session trusted under the old ones (review S-11).
 */
fun OkHttpClient.Builder.pinnedBy(pins: PinRegistry): OkHttpClient.Builder {
    val trustManager = PinningTrustManager(systemTrustManager(), pins)
    val context = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustManager), null) }
    val standard = OkHttpClient().hostnameVerifier
    val verifier = HostnameVerifier { host, session ->
        pins.pinFor(host, session.peerPort) != null || standard.verify(host, session)
    }
    val pool = ConnectionPool()
    pins.onChange {
        pool.evictAll()
        context.clientSessionContext.invalidateAll()
    }
    return sslSocketFactory(context.socketFactory, trustManager).hostnameVerifier(verifier).connectionPool(pool)
}

/** Invalidates every cached session, so no TLS handshake is resumed without checking the certificate again. */
private fun SSLSessionContext.invalidateAll() {
    for (id in ids) getSession(id)?.invalidate()
}
