package com.stacknoise.haac.feature.onboarding.ui

import com.stacknoise.haac.core.network.tls.PeerCertificate
import okhttp3.HttpUrl

/** The untrusted certificate of [url] that the user is asked to trust on first use (concept 4.3). */
data class CertificateOffer(val url: HttpUrl, val certificate: PeerCertificate)
