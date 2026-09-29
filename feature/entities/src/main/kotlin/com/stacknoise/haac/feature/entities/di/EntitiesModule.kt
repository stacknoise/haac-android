package com.stacknoise.haac.feature.entities.di

import com.stacknoise.haac.feature.entities.domain.DefaultEntityControlFactory
import com.stacknoise.haac.feature.entities.domain.DefaultHistoryChartFactory
import com.stacknoise.haac.feature.entities.domain.DefaultServiceCallFactory
import com.stacknoise.haac.feature.entities.domain.DefaultTileFactory
import com.stacknoise.haac.feature.entities.domain.EntityControlFactory
import com.stacknoise.haac.feature.entities.domain.HistoryChartFactory
import com.stacknoise.haac.feature.entities.domain.ServiceCallFactory
import com.stacknoise.haac.feature.entities.domain.TileFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Bindings of the entities feature. */
@Module
@InstallIn(SingletonComponent::class)
abstract class EntitiesModule {
    /** The tile rules of concept 7.2 and 8. */
    @Binds
    abstract fun bindTileFactory(factory: DefaultTileFactory): TileFactory

    /** The controls of concept 8. */
    @Binds
    abstract fun bindEntityControlFactory(factory: DefaultEntityControlFactory): EntityControlFactory

    /** Checked service calls (concept 8, 11.2). */
    @Binds
    abstract fun bindServiceCallFactory(factory: DefaultServiceCallFactory): ServiceCallFactory

    /** History queries and charts (concept 8.1 – 8.4). */
    @Binds
    abstract fun bindHistoryChartFactory(factory: DefaultHistoryChartFactory): HistoryChartFactory
}
