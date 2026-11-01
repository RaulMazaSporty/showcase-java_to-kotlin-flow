package io.github.rool78.java_to_kotlin_flow.domain.usecase

import io.github.rool78.java_to_kotlin_flow.domain.data.StocksRepository
import io.github.rool78.java_to_kotlin_flow.domain.model.Stock
import io.github.rool78.java_to_kotlin_flow.domain.model.WatchlistEntry
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.scan

class ObserveWatchlistUseCase @Inject constructor(
    private val repository: StocksRepository
) {
    operator fun invoke(stocks: List<Stock>): Flow<List<WatchlistEntry>> {
        val watchlist = stocks.distinctBy { it.symbol }
        val initialEntries = watchlist.map { WatchlistEntry(it) }

        return repository.observePrices(watchlist.map { it.symbol }.toSet())
            .scan(initialEntries) { entries, price ->
                entries.map { entry ->
                    if (entry.stock.symbol == price.symbol) {
                        entry.copy(price = price)
                    } else {
                        entry
                    }
                }
            }
    }
}
