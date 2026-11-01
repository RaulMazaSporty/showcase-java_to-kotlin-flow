package io.github.rool78.java_to_kotlin_flow.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.rool78.java_to_kotlin_flow.demo.DemoFeedController
import io.github.rool78.java_to_kotlin_flow.domain.usecase.ObserveWatchlistUseCase
import io.github.rool78.java_to_kotlin_flow.domain.model.WatchlistEntry
import io.github.rool78.java_to_kotlin_flow.domain.model.demoStockCatalog
import io.github.rool78.java_to_kotlin_flow.domain.model.demoWatchlist
import io.github.rool78.java_to_kotlin_flow.ui.model.WatchlistUiMapper
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StocksViewModel @Inject constructor(
    observeWatchlistUseCase: ObserveWatchlistUseCase,
    uiMapper: WatchlistUiMapper,
    private val demoFeedController: DemoFeedController,
) : ViewModel() {

    private val requests = MutableStateFlow(ObservationRequest())
    private val initialState = uiMapper.map(demoWatchlist.map { WatchlistEntry(it) }).copy(
        stockOptions = uiMapper.mapOptions(demoStockCatalog, requests.value.symbols),
    )

    val uiState = requests.flatMapLatest { request ->
        val stocks = demoStockCatalog.filter { it.symbol in request.symbols }
        val options = uiMapper.mapOptions(demoStockCatalog, request.symbols)
        observeWatchlistUseCase(stocks)
            .onEach { prices ->
                if (request.slowConsumer && prices.any { it.price != null }) delay(50.milliseconds)
            }
            .map {
                uiMapper.map(it).copy(
                    stockOptions = options,
                    isSlowConsumerEnabled = request.slowConsumer,
                )
            }
            .catch {
                emit(
                    uiMapper.map(stocks.map { WatchlistEntry(it) }).copy(
                        stockOptions = options,
                        isFeedInterrupted = true,
                        isSlowConsumerEnabled = request.slowConsumer,
                    )
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = initialState,
    )

    fun onRetryButtonClick() {
        requests.update { it.copy(retryVersion = it.retryVersion + 1) }
    }

    fun onSymbolToggle(symbol: String) {
        if (demoStockCatalog.none { it.symbol == symbol }) return
        requests.update { request ->
            request.copy(symbols = if (symbol in request.symbols) request.symbols - symbol else request.symbols + symbol)
        }
    }

    fun onInterruptFeedClick() {
        if (uiState.value.prices.isNotEmpty() && !uiState.value.isFeedInterrupted) {
            demoFeedController.interruptFeed()
        }
    }

    fun onSlowConsumerChange(enabled: Boolean) {
        requests.update { it.copy(slowConsumer = enabled) }
    }

    fun onBurstUpdatesClick() {
        if (uiState.value.prices.isNotEmpty() && !uiState.value.isFeedInterrupted) {
            demoFeedController.burstUpdates()
        }
    }

    private data class ObservationRequest(
        val symbols: Set<String> = demoWatchlist.map { it.symbol }.toSet(),
        val retryVersion: Int = 0,
        val slowConsumer: Boolean = false,
    )
}
