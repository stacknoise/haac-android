package com.stacknoise.haac.core.database.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.room.Room
import com.stacknoise.haac.core.database.HaacDatabase
import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.database.settings.AppSettings
import com.stacknoise.haac.core.database.settings.AppSettingsSerializer
import com.stacknoise.haac.core.database.settings.DataStoreActiveInstanceStore
import com.stacknoise.haac.core.database.settings.DataStoreSecuritySettings
import com.stacknoise.haac.core.database.settings.SecuritySettings
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Room database, DAOs and the settings DataStore. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {
    /** Active instance in the settings DataStore. */
    @Binds
    abstract fun bindActiveInstanceStore(store: DataStoreActiveInstanceStore): ActiveInstanceStore

    /** Security settings in the settings DataStore. */
    @Binds
    abstract fun bindSecuritySettings(settings: DataStoreSecuritySettings): SecuritySettings

    /** Singletons that need the app context. */
    companion object {
        /** The app database `haac.db`; excluded from backup by the data extraction rules. */
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): HaacDatabase =
            Room.databaseBuilder(context, HaacDatabase::class.java, "haac.db").build()

        /** DAO of the `server` table. */
        @Provides
        fun provideServerDao(database: HaacDatabase): ServerDao = database.serverDao()

        /** The single DataStore instance for [AppSettings]. */
        @Provides
        @Singleton
        fun provideSettings(@ApplicationContext context: Context): DataStore<AppSettings> =
            DataStoreFactory.create(AppSettingsSerializer) { context.dataStoreFile("app_settings.json") }
    }
}
