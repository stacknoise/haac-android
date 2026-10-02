package com.stacknoise.haac.feature.onboarding.di

import com.stacknoise.haac.feature.onboarding.data.BiometricFingerprintStep
import com.stacknoise.haac.feature.onboarding.data.HaServerValidator
import com.stacknoise.haac.feature.onboarding.data.HaSignInRepository
import com.stacknoise.haac.feature.onboarding.data.RegistryDemoEntry
import com.stacknoise.haac.feature.onboarding.domain.DemoEntry
import com.stacknoise.haac.feature.onboarding.domain.FingerprintStep
import com.stacknoise.haac.feature.onboarding.domain.ServerValidator
import com.stacknoise.haac.feature.onboarding.domain.SignInRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Bindings of the onboarding topic. */
@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {
    /** Server validation via /auth/providers. */
    @Binds
    abstract fun bindServerValidator(validator: HaServerValidator): ServerValidator

    /** Native HA login and storage of the instance. */
    @Binds
    abstract fun bindSignInRepository(repository: HaSignInRepository): SignInRepository

    /** The fingerprint offer after sign-in (concept 4.4). */
    @Binds
    abstract fun bindFingerprintStep(step: BiometricFingerprintStep): FingerprintStep

    /** The way into the demo instance (concept 20.4). */
    @Binds
    abstract fun bindDemoEntry(entry: RegistryDemoEntry): DemoEntry
}
