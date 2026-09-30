package com.stacknoise.haac.core.network.di

import com.stacknoise.haac.core.network.access.AndroidLocalNetworkAccess
import com.stacknoise.haac.core.network.access.LocalNetworkAccess
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Bindings of the local network permission (concept 4.2). */
@Module
@InstallIn(SingletonComponent::class)
abstract class AccessBindings {
    /** The state of `ACCESS_LOCAL_NETWORK`. */
    @Binds
    abstract fun bindLocalNetworkAccess(access: AndroidLocalNetworkAccess): LocalNetworkAccess
}
