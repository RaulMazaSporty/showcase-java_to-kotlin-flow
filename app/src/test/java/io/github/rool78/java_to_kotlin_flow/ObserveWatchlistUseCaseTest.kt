package io.github.rool78.java_to_kotlin_flow

import io.github.rool78.java_to_kotlin_flow.domain.data.StocksRepository
import io.github.rool78.java_to_kotlin_flow.domain.model.Stock
import io.github.rool78.java_to_kotlin_flow.domain.model.WatchlistEntry
import io.github.rool78.java_to_kotlin_flow.domain.model.StockPrice
import io.github.rool78.java_to_kotlin_flow.domain.usecase.ObserveWatchlistUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ObserveWatchlistUseCaseTest {

    @Test
    fun `updates retain other symbols and each collection has independent state`() = runTest {
        val apple = Stock("AAPL", "Apple")
        val microsoft = Stock("MSFT", "Microsoft")
        val firstApple = StockPrice("AAPL", 100, 90, 1)
        val updatedApple = firstApple.copy(priceInCents = 110, timestampMillis = 3)
        val msftPrice = StockPrice("MSFT", 200, 190, 2)
        var requested: Set<String>? = null
        val repo = object : StocksRepository {
            override fun observePrices(symbols: Set<String>) = flowOf(firstApple, msftPrice, updatedApple).also {
                requested = symbols
            }
        }
        val flow = ObserveWatchlistUseCase(repo)(listOf(apple, microsoft))
        val snapshots = flow.toList()
        assertEquals(setOf("AAPL", "MSFT"), requested)
        assertEquals(listOf(WatchlistEntry(apple), WatchlistEntry(microsoft)), snapshots.first())
        assertEquals(listOf(WatchlistEntry(apple, updatedApple), WatchlistEntry(microsoft, msftPrice)), snapshots.last())
        assertEquals(firstApple, snapshots[1][0].price)
        assertEquals(snapshots, flow.toList())
    }
}
