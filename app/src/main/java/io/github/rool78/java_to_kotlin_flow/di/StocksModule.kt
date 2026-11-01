package io.github.rool78.java_to_kotlin_flow.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.rool78.java_to_kotlin_flow.demo.DemoFeedController
import io.github.rool78.java_to_kotlin_flow.demo.SimulatedFeedController
import io.github.rool78.java_to_kotlin_flow.data.StocksRepositoryImpl
import io.github.rool78.java_to_kotlin_flow.domain.data.StocksRepository
import io.github.rool78.java_to_kotlin_flow.legacy.LegacyStockManager
import io.github.rool78.java_to_kotlin_flow.legacy.SimulatedLegacyStockManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StocksModule {

    @Binds
    abstract fun bindStocksRepository(implementation: StocksRepositoryImpl): StocksRepository

    @Binds
    abstract fun bindDemoFeedController(implementation: SimulatedFeedController): DemoFeedController

    companion object {
        @Provides
        @Singleton
        fun provideSimulatedStockManager(): SimulatedLegacyStockManager = SimulatedLegacyStockManager()

        @Provides
        fun provideLegacyStockManager(manager: SimulatedLegacyStockManager): LegacyStockManager = manager
    }
}
