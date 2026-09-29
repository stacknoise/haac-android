package com.stacknoise.haac.core.network.di

import android.content.Context
import android.net.nsd.NsdManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import com.stacknoise.haac.core.network.tls.PinRegistry
import com.stacknoise.haac.core.network.tls.pinnedBy
import okhttp3.OkHttpClient

/** Shared HTTP client and JSON settings; no logging interceptor, so auth requests are never logged (concept 5.1). */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val TIMEOUT_SECONDS = 10L

    /** The one client of the app; trusts by pin or by system certificates (concept 4.3). */
    @Provides
    @Singleton
    fun provideOkHttpClient(pins: PinRegistry): OkHttpClient = OkHttpClient.Builder()
        .pinnedBy(pins)
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    /** JSON that tolerates new fields from future HA versions. */
    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    /** The system NsdManager for LAN discovery (concept 4.2, 4.5). */
    @Provides
    fun provideNsdManager(@ApplicationContext context: Context): NsdManager =
        context.getSystemService(NsdManager::class.java)

    /** Scope of [com.stacknoise.haac.core.network.connection.ConnectionSupervisor]; lives as long as the app. */
    @Provides
    @Singleton
    @ConnectionScope
    fun provideConnectionScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}
