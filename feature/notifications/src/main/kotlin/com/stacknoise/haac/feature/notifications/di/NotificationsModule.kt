package com.stacknoise.haac.feature.notifications.di

import com.stacknoise.haac.core.common.sync.EntityChangeReporter
import com.stacknoise.haac.core.common.sync.ScheduleChangeReporter
import com.stacknoise.haac.core.error.ErrorReporter
import com.stacknoise.haac.feature.notifications.data.NotificationReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The notification list receives errors and sync changes from other topics (concept 17.4). */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {
    /** Errors of every topic. */
    @Binds
    abstract fun bindErrorReporter(reporter: NotificationReporter): ErrorReporter

    /** Entity changes of the sync. */
    @Binds
    abstract fun bindEntityChangeReporter(reporter: NotificationReporter): EntityChangeReporter

    /** Schedule changes of the sync. */
    @Binds
    abstract fun bindScheduleChangeReporter(reporter: NotificationReporter): ScheduleChangeReporter
}
