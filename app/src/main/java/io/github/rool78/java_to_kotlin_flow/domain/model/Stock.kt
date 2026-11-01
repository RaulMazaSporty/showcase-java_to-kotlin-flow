package io.github.rool78.java_to_kotlin_flow.domain.model

data class Stock(val symbol: String, val name: String)

data class StockPrice(
    val symbol: String,
    val priceInCents: Long,
    val previousCloseInCents: Long,
    val timestampMillis: Long,
)

data class WatchlistEntry(val stock: Stock, val price: StockPrice? = null)

val demoWatchlist = listOf(
    Stock("AAPL", "Apple"),
    Stock("MSFT", "Microsoft"),
    Stock("GOOGL", "Alphabet"),
)

val demoStockCatalog = demoWatchlist + listOf(
    Stock("NVDA", "NVIDIA"),
    Stock("AMZN", "Amazon"),
)
