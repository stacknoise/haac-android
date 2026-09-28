package com.stacknoise.haac.feature.onboarding.di

import android.content.Context
import android.net.nsd.NsdManager
import com.stacknoise.haac.feature.onboarding.data.HaServerValidator
import com.stacknoise.haac.feature.onboarding.data.HaSignInRepository
import com.stacknoise.haac.feature.onboarding.data.NsdServerDiscovery
import com.stacknoise.haac.feature.onboarding.domain.ServerDiscovery
import com.stacknoise.haac.feature.onboarding.domain.ServerValidator
import com.stacknoise.haac.feature.onboarding.domain.SignInRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/** Bindings of the onboarding topic. */
@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {
    /** LAN discovery via NsdManager. */
    @Binds
    abstract fun bindServerDiscovery(discovery: NsdServerDiscovery): ServerDiscovery

    /** Server validation via /auth/providers. */
    @Binds
    abstract fun bindServerValidator(validator: HaServerValidator): ServerValidator

    /** Native HA login and storage of the instance. */
    @Binds
    abstract fun bindSignInRepository(repository: HaSignInRepository): SignInRepository

    /** System services used by the onboarding topic. */
    companion object {
        /** The system NsdManager. */
        @Provides
        fun provideNsdManager(@ApplicationContext context: Context): NsdManager =
            context.getSystemService(NsdManager::class.java)
    }
}
