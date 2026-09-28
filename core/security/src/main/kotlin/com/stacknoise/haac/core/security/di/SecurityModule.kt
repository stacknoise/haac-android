package com.stacknoise.haac.core.security.di

import android.content.Context
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.security.biometric.AndroidBiometricPrompter
import com.stacknoise.haac.core.security.biometric.BiometricPrompter
import com.stacknoise.haac.core.security.biometric.FingerprintUnlock
import com.stacknoise.haac.core.security.biometric.KeystoreFingerprintUnlock
import com.stacknoise.haac.core.security.keystore.AesGcmCipherFactory
import com.stacknoise.haac.core.security.keystore.AndroidKeyFactory
import com.stacknoise.haac.core.security.keystore.CipherFactory
import com.stacknoise.haac.core.security.keystore.KeyFactory
import com.stacknoise.haac.core.security.token.KeystoreTokenStore
import com.stacknoise.haac.core.security.token.TokenFiles
import com.stacknoise.haac.core.security.token.TokenStore
import com.stacknoise.haac.core.security.token.UnlockedTokens
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

    /** AndroidX BiometricPrompt. */
    @Binds
    abstract fun bindBiometricPrompter(prompter: AndroidBiometricPrompter): BiometricPrompter

    /** Fingerprint unlock with Keystore keys. */
    @Binds
    abstract fun bindFingerprintUnlock(unlock: KeystoreFingerprintUnlock): FingerprintUnlock

    /** Objects that need the app context. */
    companion object {
        /** Token files live in no-backup storage, so they never reach a cloud backup (concept 5.3). */
        @Provides
        @Singleton
        fun provideTokenFiles(@ApplicationContext context: Context): TokenFiles =
            TokenFiles(File(context.noBackupFilesDir, "tokens"))

        /** The token store shared by all instances. */
        @Provides
        @Singleton
        fun provideTokenStore(
            files: TokenFiles,
            keys: KeyFactory,
            ciphers: CipherFactory,
            unlocked: UnlockedTokens,
            errors: ErrorFactory,
        ): TokenStore = KeystoreTokenStore(files, keys, ciphers, unlocked, errors)
    }
}
