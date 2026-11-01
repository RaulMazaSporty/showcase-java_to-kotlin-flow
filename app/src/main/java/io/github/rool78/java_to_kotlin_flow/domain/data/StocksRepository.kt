package io.github.rool78.java_to_kotlin_flow.domain.data

import io.github.rool78.java_to_kotlin_flow.domain.model.StockPrice
import kotlinx.coroutines.flow.Flow

class PriceBufferOverflowException : IllegalStateException("Price consumer cannot keep up")

interface StocksRepository {
    fun observePrices(symbols: Set<String>): Flow<StockPrice>
}
