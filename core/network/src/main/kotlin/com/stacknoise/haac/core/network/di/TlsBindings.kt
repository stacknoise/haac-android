package com.stacknoise.haac.core.network.di

import com.stacknoise.haac.core.network.tls.CertificateProbe
import com.stacknoise.haac.core.network.tls.TlsCertificateProbe
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Bindings of the certificate pinning (concept 4.3). */
@Module
@InstallIn(SingletonComponent::class)
abstract class TlsBindings {
    /** Reads the certificate of a server for trust on first use. */
    @Binds
    abstract fun bindCertificateProbe(probe: TlsCertificateProbe): CertificateProbe
}
