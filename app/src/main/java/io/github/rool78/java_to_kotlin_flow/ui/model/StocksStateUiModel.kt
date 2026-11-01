package io.github.rool78.java_to_kotlin_flow.ui.model

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

data class StockPriceUiModel(
    val symbol: String,
    val companyName: String,
    val formattedPrice: String? = null,
    val formattedChange: String? = null,
    val isPriceDown: Boolean = false,
)

data class StocksStateUiModel(
    val prices: PersistentList<StockPriceUiModel> = persistentListOf(),
    val stockOptions: PersistentList<StockOptionUiModel> = persistentListOf(),
    val isFeedInterrupted: Boolean = false,
    val isSlowConsumerEnabled: Boolean = false,
) {
    val isLoading: Boolean get() = !isFeedInterrupted && prices.isNotEmpty() && prices.all { it.formattedPrice == null }
}

data class StockOptionUiModel(
    val symbol: String,
    val companyName: String,
    val isSelected: Boolean,
)
