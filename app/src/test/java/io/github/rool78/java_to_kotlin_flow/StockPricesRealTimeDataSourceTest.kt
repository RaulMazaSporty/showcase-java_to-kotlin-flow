package io.github.rool78.java_to_kotlin_flow

import io.github.rool78.java_to_kotlin_flow.data.StockPricesRealTimeDataSource
import io.github.rool78.java_to_kotlin_flow.domain.data.PriceBufferOverflowException
import io.github.rool78.java_to_kotlin_flow.legacy.model.StockPriceDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StockPricesRealTimeDataSourceTest {

    @Test
    fun `collection is lazy and cancellation removes exactly its listeners`() = runTest {
        val manager = FakeLegacyStockManager()
        val flow = StockPricesRealTimeDataSource(manager).observe(setOf("AAPL", "MSFT"))
        assertTrue(manager.registrations.isEmpty())
        val job = backgroundScope.launch { flow.collect() }
        runCurrent()
        val registered = manager.registrations.toSet()
        assertEquals(setOf("AAPL", "MSFT"), registered.map { it.first }.toSet())
        job.cancelAndJoin()
        assertTrue(manager.registrations.isEmpty())
        assertEquals(registered, manager.removed.toSet())
    }

    @Test
    fun `first receives a callback from another thread and releases the listener`() = runTest {
        val manager = FakeLegacyStockManager()
        val result = async { StockPricesRealTimeDataSource(manager).observe(setOf("AAPL")).first() }
        runCurrent()
        val listener = manager.registrations.single().second
        Thread {
            listener.onPrice(
                StockPriceDto(
                    "AAPL",
                    12345,
                    12000,
                    42
                )
            )
        }.apply { start(); join() }
        val price = result.await()
        assertEquals(12345L, price.priceInCents)
        assertEquals(42L, price.timestampMillis)
        assertTrue(manager.registrations.isEmpty())
    }

    @Test
    fun `two cold collectors own separate listeners`() = runTest {
        val manager = FakeLegacyStockManager()
        val flow = StockPricesRealTimeDataSource(manager).observe(setOf("AAPL"))
        val first = backgroundScope.launch { flow.collect() }
        val second = backgroundScope.launch { flow.collect() }
        runCurrent()
        assertEquals(2, manager.registrations.size)
        first.cancelAndJoin()
        assertEquals(1, manager.registrations.size)
        second.cancelAndJoin()
        assertTrue(manager.registrations.isEmpty())
    }

    @Test
    fun `registration failure cleans earlier and partially registered listeners`() = runTest {
        val manager = FakeLegacyStockManager()
        val failure = IllegalStateException("Failed after registering MSFT")
        manager.onSubscribe = { symbol, _ -> if (symbol == "MSFT") throw failure }
        val error = runCatching {
            StockPricesRealTimeDataSource(manager).observe(linkedSetOf("AAPL", "MSFT")).collect()
        }.exceptionOrNull()
        // Coroutine stack-trace recovery may copy exceptions across suspension boundaries.
        assertEquals(failure.javaClass, error?.javaClass)
        assertEquals(failure.message, error?.message)
        assertTrue(manager.registrations.isEmpty())
        assertEquals(listOf("AAPL", "MSFT"), manager.removed.map { it.first })
    }

    @Test
    fun `late callbacks after cancellation are harmless`() = runTest {
        val manager = FakeLegacyStockManager()
        val received = mutableListOf<StockPriceDto>()
        val job = backgroundScope.launch {
            StockPricesRealTimeDataSource(manager).observe(setOf("AAPL")).toList(received)
        }
        runCurrent()
        val listener = manager.registrations.single().second
        job.cancelAndJoin()
        listener.onPrice(StockPriceDto("AAPL", 100, 100, 0))
        listener.onError(IllegalStateException("late failure"))
        runCurrent()
        assertTrue(received.isEmpty())
    }

    @Test
    fun `feed failure propagates and releases all registrations`() = runTest {
        val manager = FakeLegacyStockManager()
        var caught: Throwable? = null
        val job = backgroundScope.launch {
            StockPricesRealTimeDataSource(manager).observe(setOf("AAPL", "MSFT"))
                .catch { caught = it }.collect()
        }
        runCurrent()
        val failure = IllegalStateException("Disconnected")
        manager.fail(failure)
        job.join()
        assertEquals(failure.javaClass, caught?.javaClass)
        assertEquals(failure.message, caught?.message)
        assertTrue(manager.registrations.isEmpty())
    }

    @Test
    fun `overflow is explicit instead of silently dropping updates`() = runTest {
        val manager = FakeLegacyStockManager()
        manager.onSubscribe = { symbol, listener ->
            repeat(200) { listener.onPrice(StockPriceDto(symbol, it.toLong(), 100, 0)) }
        }
        val error = runCatching {
            StockPricesRealTimeDataSource(manager).observe(setOf("AAPL")).collect()
        }.exceptionOrNull()
        assertTrue(error is PriceBufferOverflowException)
        assertTrue(manager.registrations.isEmpty())
    }

    @Test
    fun `empty selection completes without registering`() = runTest {
        val manager = FakeLegacyStockManager()
        assertTrue(StockPricesRealTimeDataSource(manager).observe(emptySet()).toList().isEmpty())
        assertTrue(manager.registrations.isEmpty())
    }
}
