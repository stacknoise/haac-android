package com.stacknoise.haac.core.security.di

import android.content.Context
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.security.keystore.AesGcmCipherFactory
import com.stacknoise.haac.core.security.keystore.AndroidKeyFactory
import com.stacknoise.haac.core.security.keystore.CipherFactory
import com.stacknoise.haac.core.security.keystore.KeyFactory
import com.stacknoise.haac.core.security.token.KeystoreTokenStore
import com.stacknoise.haac.core.security.token.TokenStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/** Bindings of the security topic. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {
    /** Keys in the Android Keystore. */
    @Binds
    abstract fun bindKeyFactory(factory: AndroidKeyFactory): KeyFactory

    /** AES-GCM ciphers. */
    @Binds
    abstract fun bindCipherFactory(factory: AesGcmCipherFactory): CipherFactory

    /** Objects that need the app context. */
    companion object {
        /** Token files live in no-backup storage, so they never reach a cloud backup (concept 5.3). */
        @Provides
        @Singleton
        fun provideTokenStore(
            @ApplicationContext context: Context,
            keys: KeyFactory,
            ciphers: CipherFactory,
            errors: ErrorFactory,
        ): TokenStore = KeystoreTokenStore(File(context.noBackupFilesDir, "tokens"), keys, ciphers, errors)
    }
}
