package io.github.rool78.java_to_kotlin_flow.domain.mapper

import io.github.rool78.java_to_kotlin_flow.domain.model.StockPrice
import io.github.rool78.java_to_kotlin_flow.legacy.model.StockPriceDto
import javax.inject.Inject

class StockPriceDomainMapper @Inject constructor() {

    fun map(dto: StockPriceDto): StockPrice = StockPrice(
        symbol = dto.symbol,
        priceInCents = dto.priceInCents,
        previousCloseInCents = dto.previousCloseInCents,
        timestampMillis = dto.timestampMillis,
    )
}
