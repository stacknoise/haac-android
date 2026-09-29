package com.stacknoise.haac.feature.settings.ui

import com.stacknoise.haac.core.common.ui.CertificateDialogKind
import com.stacknoise.haac.core.network.endpoint.AddressSlot
import com.stacknoise.haac.core.network.tls.PeerCertificate
import okhttp3.HttpUrl

/**
 * The certificate dialog of an address (concept 4.3): [certificate] is what [url] presents now, [kind] says
 * what is asked. [saving] is set while a new address waits for the user to trust its certificate.
 */
data class CertificateReview(
    val slot: AddressSlot,
    val url: HttpUrl,
    val certificate: PeerCertificate,
    val kind: CertificateDialogKind,
    val saving: Boolean = false,
)
