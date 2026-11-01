package io.github.rool78.java_to_kotlin_flow

import io.github.rool78.java_to_kotlin_flow.domain.model.Stock
import io.github.rool78.java_to_kotlin_flow.domain.model.WatchlistEntry
import io.github.rool78.java_to_kotlin_flow.domain.model.StockPrice
import io.github.rool78.java_to_kotlin_flow.ui.model.WatchlistUiMapper
import org.junit.Assert.*
import org.junit.Test

class WatchlistUiMapperTest {
    private val mapper = WatchlistUiMapper()

    @Test fun `formats USD cents and signed changes including an unchanged price`() {
        val stock = Stock("AAPL", "Apple")
        val state = mapper.map(listOf(12345L, 11900L, 12000L).map { cents ->
            WatchlistEntry(stock, StockPrice(stock.symbol, cents, 12000, 1))
        })
        assertEquals(listOf("$123.45", "$119.00", "$120.00"), state.prices.map { it.formattedPrice })
        assertEquals(listOf("+$3.45", "-$1.00", "+$0.00"), state.prices.map { it.formattedChange })
        assertEquals(listOf(false, true, false), state.prices.map { it.isPriceDown })
        assertFalse(state.isLoading)
    }

    @Test fun `pending prices retain company identity without fabricating a price`() {
        val state = mapper.map(listOf(WatchlistEntry(Stock("MSFT", "Microsoft"))))
        assertEquals("MSFT", state.prices.single().symbol)
        assertEquals("Microsoft", state.prices.single().companyName)
        assertNull(state.prices.single().formattedPrice)
        assertNull(state.prices.single().formattedChange)
        assertTrue(state.isLoading)
    }
}
