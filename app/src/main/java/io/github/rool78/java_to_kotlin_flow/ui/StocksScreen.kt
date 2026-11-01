package io.github.rool78.java_to_kotlin_flow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rool78.java_to_kotlin_flow.R
import io.github.rool78.java_to_kotlin_flow.ui.presentation.StocksViewModel
import io.github.rool78.java_to_kotlin_flow.ui.model.StockPriceUiModel
import io.github.rool78.java_to_kotlin_flow.ui.model.StocksStateUiModel
import io.github.rool78.java_to_kotlin_flow.ui.theme.DemoTheme

@Composable
fun StocksRoute(viewModel: StocksViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StocksScreen(
        state = state,
        onRetry = viewModel::onRetryButtonClick,
        onSymbolToggle = viewModel::onSymbolToggle,
        onInterruptFeed = viewModel::onInterruptFeedClick,
        onBurstUpdates = viewModel::onBurstUpdatesClick,
        onSlowConsumerChange = viewModel::onSlowConsumerChange,
    )
}

@Composable
fun StocksScreen(
    state: StocksStateUiModel,
    onRetry: () -> Unit,
    onSymbolToggle: (String) -> Unit = {},
    onInterruptFeed: () -> Unit = {},
    onBurstUpdates: () -> Unit = {},
    onSlowConsumerChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            DemoControlsPanel(state, onSymbolToggle, onInterruptFeed, onBurstUpdates, onSlowConsumerChange)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("watchlist")
                .consumeWindowInsets(padding),
            contentPadding = padding,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.market_watch),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.watchlist_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.watchlist_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            stringResource(R.string.simulated_feed),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (state.isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            stringResource(R.string.waiting_for_prices),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            if (state.isFeedInterrupted) {
                item {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(stringResource(R.string.feed_unavailable))
                            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                        }
                    }
                }
            }
            if (state.prices.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.empty_watchlist),
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.prices, key = { it.symbol }) { price ->
                PriceCard(
                    price,
                    Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }
            item {
                Text(
                    stringResource(R.string.demo_disclaimer),
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(24.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PriceCard(
    price: StockPriceUiModel,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.testTag("price_${price.symbol}"),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    price.symbol,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    price.companyName, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                Modifier.weight(1f), horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(price.formattedPrice
                    ?: "—",
                    style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.End,
                    fontWeight = FontWeight.SemiBold)
                Text(
                    price.formattedChange?.let { stringResource(R.string.change_since_close, it) }
                        ?: stringResource(R.string.no_price_yet), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End,
                    color = if (price.isPriceDown) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DemoControlsPanel(
    state: StocksStateUiModel,
    onSymbolToggle: (String) -> Unit,
    onInterruptFeed: () -> Unit,
    onBurstUpdates: () -> Unit,
    onSlowConsumerChange: (Boolean) -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
                    .heightIn(max = 320.dp).verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.demo_controls), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.select_companies), style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.stockOptions.forEach { stock ->
                        FilterChip(
                            selected = stock.isSelected,
                            onClick = { onSymbolToggle(stock.symbol) },
                            label = { Text(stock.symbol) },
                            modifier = Modifier.testTag("stock_option_${stock.symbol}")
                                .semantics { contentDescription = "${stock.companyName} (${stock.symbol})" },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.slow_processing), style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(R.string.slow_processing_detail), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = state.isSlowConsumerEnabled,
                        onCheckedChange = onSlowConsumerChange,
                        modifier = Modifier.testTag("slow_processing").semantics {
                            contentDescription = "Slow processing"
                        },
                    )
                }
                Text(stringResource(R.string.burst_hint), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onInterruptFeed,
                        enabled = state.prices.isNotEmpty() && !state.isFeedInterrupted,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.interrupt_feed)) }
                    FilledTonalButton(
                        onClick = onBurstUpdates,
                        enabled = state.prices.isNotEmpty() && !state.isFeedInterrupted,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.burst_updates)) }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Live prices")
@Composable
private fun StocksPreview() {
    DemoTheme(dynamicColor = false) {
        StocksScreen(
            StocksStateUiModel(
                prices = persistentListOf(
                    StockPriceUiModel("AAPL", "Apple", "$215.35", "+$0.35"),
                    StockPriceUiModel("MSFT", "Microsoft", "$419.00", "-$1.00", true),
                    StockPriceUiModel("GOOGL", "Alphabet", "$175.00", "+$0.00"),
                )
            ), onRetry = {})
    }
}

@Preview(showBackground = true, name = "Waiting", fontScale = 1.5f)
@Composable
private fun LoadingPreview() {
    DemoTheme(dynamicColor = false) {
        StocksScreen(
            StocksStateUiModel(),
            {})
    }
}

@Preview(showBackground = true, name = "Error · dark")
@Composable
private fun ErrorPreview() {
    DemoTheme(darkTheme = true, dynamicColor = false) {
        StocksScreen(StocksStateUiModel(isFeedInterrupted = true), {})
    }
}
