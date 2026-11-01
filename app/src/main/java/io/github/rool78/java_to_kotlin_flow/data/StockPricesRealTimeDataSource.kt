package io.github.rool78.java_to_kotlin_flow.data

import io.github.rool78.java_to_kotlin_flow.legacy.model.StockPriceDto
import io.github.rool78.java_to_kotlin_flow.domain.data.PriceBufferOverflowException
import javax.inject.Inject
import io.github.rool78.java_to_kotlin_flow.legacy.LegacyStockManager
import io.github.rool78.java_to_kotlin_flow.legacy.StockPriceListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

class StockPricesRealTimeDataSource @Inject constructor(
    private val manager: LegacyStockManager
) {

    fun observe(symbols: Set<String>): Flow<StockPriceDto> {
        val requestedSymbols = symbols.toSet()
        return callbackFlow {
            if (requestedSymbols.isEmpty()) {
                close()
                return@callbackFlow
            }
            val registered = mutableSetOf<String>()
            val listener = object : StockPriceListener {
                override fun onPrice(price: StockPriceDto) {
                    if (price.symbol !in requestedSymbols) return
                    val result = trySend(price)
                    // as showcase example we close channel if consumer can't handle
                    if (result.isFailure && !result.isClosed) {
                        close(PriceBufferOverflowException())
                    }
                }

                override fun onError(cause: Throwable) {
                    close(cause)
                }
            }
            try {
                requestedSymbols.forEach { symbol ->
                    registered.add(symbol)
                    manager.subscribe(symbol, listener)
                }
                awaitClose()
            } finally {
                registered.forEach { manager.unsubscribe(it, listener) }
            }
        }.buffer(capacity = 64)
    }
}
