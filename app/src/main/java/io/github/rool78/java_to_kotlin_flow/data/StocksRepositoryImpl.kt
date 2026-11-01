package io.github.rool78.java_to_kotlin_flow.data

import io.github.rool78.java_to_kotlin_flow.domain.model.StockPrice
import io.github.rool78.java_to_kotlin_flow.domain.mapper.StockPriceDomainMapper
import io.github.rool78.java_to_kotlin_flow.domain.data.StocksRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StocksRepositoryImpl @Inject constructor(
    private val dataSource: StockPricesRealTimeDataSource,
    private val domainMapper: StockPriceDomainMapper,
) : StocksRepository {

    override fun observePrices(symbols: Set<String>): Flow<StockPrice> =
        dataSource.observe(symbols).map(domainMapper::map)
}
