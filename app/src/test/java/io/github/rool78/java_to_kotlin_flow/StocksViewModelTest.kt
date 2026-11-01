package io.github.rool78.java_to_kotlin_flow

import androidx.lifecycle.ViewModelStore
import io.github.rool78.java_to_kotlin_flow.data.StocksRepositoryImpl
import io.github.rool78.java_to_kotlin_flow.data.StockPricesRealTimeDataSource
import io.github.rool78.java_to_kotlin_flow.domain.usecase.ObserveWatchlistUseCase
import io.github.rool78.java_to_kotlin_flow.ui.model.WatchlistUiMapper
import io.github.rool78.java_to_kotlin_flow.domain.mapper.StockPriceDomainMapper
import io.github.rool78.java_to_kotlin_flow.demo.DemoFeedController
import io.github.rool78.java_to_kotlin_flow.domain.data.PriceBufferOverflowException
import io.github.rool78.java_to_kotlin_flow.ui.presentation.StocksViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StocksViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val manager = FakeLegacyStockManager()
    private lateinit var viewModel: StocksViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = StocksViewModel(
            ObserveWatchlistUseCase(
                StocksRepositoryImpl(
                    StockPricesRealTimeDataSource(manager), StockPriceDomainMapper()
                )
            ), WatchlistUiMapper(), object : DemoFeedController {
                override fun interruptFeed() { manager.fail(IllegalStateException("Demo interruption")) }
                override fun burstUpdates() {
                    repeat(256) {
                        manager.registrations.map { it.first }.distinct().forEach { manager.emit(it) }
                    }
                }
            }
        )
        store.put("stocks", viewModel)
    }

    @After
    fun tearDown() {
        store.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    @Test
    fun `state shares one registration set and stops five seconds after last collector`() =
        runTest(dispatcher) {
            assertTrue(manager.registrations.isEmpty())
            val first = backgroundScope.launch { viewModel.uiState.collect() }
            val second = backgroundScope.launch { viewModel.uiState.collect() }
            runCurrent()
            assertEquals(3, manager.registrations.size)
            manager.emit("AAPL", 100)
            runCurrent()
            assertEquals("$1.00", viewModel.uiState.value.prices.first().formattedPrice)
            first.cancel()
            runCurrent()
            assertEquals(3, manager.registrations.size)
            second.cancel()
            runCurrent()
            advanceTimeBy(4999)
            assertEquals(3, manager.registrations.size)
            advanceTimeBy(1)
            runCurrent()
            assertTrue(manager.registrations.isEmpty())
            assertEquals("$1.00", viewModel.uiState.value.prices.first().formattedPrice)
            backgroundScope.launch { viewModel.uiState.collect() }
            runCurrent()
            assertEquals(3, manager.registrations.size)
        }

    @Test
    fun `failure becomes UI state and retry registers a fresh observation`() = runTest(dispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        runCurrent()
        manager.fail(IllegalStateException("Disconnected"))
        runCurrent()
        assertTrue(viewModel.uiState.value.isFeedInterrupted)
        assertTrue(manager.registrations.isEmpty())
        viewModel.onRetryButtonClick()
        runCurrent()
        assertEquals(3, manager.registrations.size)
        assertFalse(viewModel.uiState.value.isFeedInterrupted)
        manager.emit("AAPL", 123)
        runCurrent()
        assertEquals("$1.23", viewModel.uiState.value.prices.first().formattedPrice)
    }

    @Test
    fun `clearing ViewModel removes listeners immediately`() = runTest(dispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        runCurrent()
        store.clear()
        runCurrent()
        assertTrue(manager.registrations.isEmpty())
    }

    @Test
    fun `buffer overflow becomes the same generic interruption state`() = runTest(dispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        runCurrent()
        manager.fail(PriceBufferOverflowException())
        runCurrent()
        assertTrue(viewModel.uiState.value.isFeedInterrupted)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(manager.registrations.isEmpty())
    }

    @Test fun `selection replaces listeners and supports an empty watchlist`() = runTest(dispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        runCurrent()
        val originalRegistrations = manager.registrations.toSet()
        viewModel.onSymbolToggle("NVDA")
        runCurrent()
        assertEquals(setOf("AAPL", "MSFT", "GOOGL", "NVDA"), manager.registrations.map { it.first }.toSet())
        assertTrue(manager.removed.containsAll(originalRegistrations))
        manager.emit("NVDA", 14000)
        runCurrent()
        assertEquals("$140.00", viewModel.uiState.value.prices.single { it.symbol == "NVDA" }.formattedPrice)
        viewModel.onSymbolToggle("AAPL")
        runCurrent()
        assertFalse(manager.registrations.any { it.first == "AAPL" })
        assertFalse(viewModel.uiState.value.prices.any { it.symbol == "AAPL" })
        listOf("MSFT", "GOOGL", "NVDA").forEach {
            viewModel.onSymbolToggle(it)
            runCurrent()
        }
        assertTrue(manager.registrations.isEmpty())
        assertTrue(viewModel.uiState.value.prices.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.stockOptions.any { it.isSelected })
    }

    @Test fun `demo interruption uses callbacks and reconnect preserves selection`() = runTest(dispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        runCurrent()
        viewModel.onSymbolToggle("AMZN")
        runCurrent()
        viewModel.onInterruptFeedClick()
        runCurrent()
        assertTrue(viewModel.uiState.value.isFeedInterrupted)
        assertTrue(manager.registrations.isEmpty())
        viewModel.onRetryButtonClick()
        runCurrent()
        assertEquals(setOf("AAPL", "MSFT", "GOOGL", "AMZN"), manager.registrations.map { it.first }.toSet())
        assertFalse(viewModel.uiState.value.isFeedInterrupted)
    }

    @Test fun `slow processing and burst reach overflow through price callbacks`() = runTest(dispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        runCurrent()
        viewModel.onSlowConsumerChange(true)
        runCurrent()
        assertTrue(viewModel.uiState.value.isSlowConsumerEnabled)
        viewModel.onBurstUpdatesClick()
        runCurrent()
        advanceTimeBy(4_000)
        runCurrent()
        assertTrue(viewModel.uiState.value.isFeedInterrupted)
        assertTrue(manager.registrations.isEmpty())
        viewModel.onRetryButtonClick()
        runCurrent()
        assertFalse(viewModel.uiState.value.isFeedInterrupted)
        assertTrue(viewModel.uiState.value.isSlowConsumerEnabled)
        assertEquals(3, manager.registrations.size)
    }
}
