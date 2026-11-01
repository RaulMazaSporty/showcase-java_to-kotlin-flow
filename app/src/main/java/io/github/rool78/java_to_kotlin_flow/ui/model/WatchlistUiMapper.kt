package io.github.rool78.java_to_kotlin_flow.ui.model

import io.github.rool78.java_to_kotlin_flow.domain.model.WatchlistEntry
import io.github.rool78.java_to_kotlin_flow.domain.model.Stock
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.collections.immutable.toPersistentList

class WatchlistUiMapper @Inject constructor() {

    fun mapOptions(stocks: List<Stock>, selectedSymbols: Set<String>) = stocks.map {
        StockOptionUiModel(it.symbol, it.name, it.symbol in selectedSymbols)
    }.toPersistentList()

    fun map(prices: List<WatchlistEntry>): StocksStateUiModel {
        val currency = NumberFormat.getCurrencyInstance(Locale.US)
        return StocksStateUiModel(prices = prices.map { entry ->
            val price = entry.price
            val change = price?.let { it.priceInCents - it.previousCloseInCents }
            StockPriceUiModel(
                symbol = entry.stock.symbol,
                companyName = entry.stock.name,
                formattedPrice = price?.let {
                    currency.format(
                        BigDecimal.valueOf(
                            it.priceInCents,
                            2
                        )
                    )
                },
                formattedChange = change?.let {
                    val amount = currency.format(BigDecimal.valueOf(it, 2))
                    if (it >= 0) "+$amount" else amount
                },
                isPriceDown = change != null && change < 0,
            )
        }.toPersistentList())
    }
}
