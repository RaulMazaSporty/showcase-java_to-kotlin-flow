package io.github.rool78.java_to_kotlin_flow

import io.github.rool78.java_to_kotlin_flow.legacy.model.StockPriceDto
import io.github.rool78.java_to_kotlin_flow.legacy.LegacyStockManager
import io.github.rool78.java_to_kotlin_flow.legacy.StockPriceListener

class FakeLegacyStockManager : LegacyStockManager {
    val registrations = linkedSetOf<Pair<String, StockPriceListener>>()
    val removed = mutableListOf<Pair<String, StockPriceListener>>()
    var onSubscribe: (String, StockPriceListener) -> Unit = { _, _ -> }

    override fun subscribe(symbol: String, listener: StockPriceListener) {
        registrations.add(symbol to listener)
        onSubscribe(symbol, listener)
    }

    override fun unsubscribe(symbol: String, listener: StockPriceListener) {
        registrations.remove(symbol to listener)
        removed.add(symbol to listener)
    }

    fun emit(symbol: String, cents: Long = 100) {
        registrations.toList().filter { it.first == symbol }.forEach { (_, listener) ->
            listener.onPrice(StockPriceDto(symbol, cents, 100, 1))
        }
    }

    fun fail(cause: Throwable) {
        registrations.map { it.second }.distinct().forEach { it.onError(cause) }
    }
}
