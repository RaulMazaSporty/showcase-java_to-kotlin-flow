package io.github.rool78.java_to_kotlin_flow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.rool78.java_to_kotlin_flow.ui.model.StockPriceUiModel
import io.github.rool78.java_to_kotlin_flow.ui.StocksScreen
import io.github.rool78.java_to_kotlin_flow.ui.model.StocksStateUiModel
import io.github.rool78.java_to_kotlin_flow.ui.theme.DemoTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StocksScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun displaysPriceAndIdentifiesSimulatedData() {
        compose.setContent {
            DemoTheme {
                StocksScreen(
                    StocksStateUiModel(
                        prices = persistentListOf(
                            StockPriceUiModel("AAPL", "Apple", "$123.45", "+$3.45"),
                        )
                    ), onRetry = {})
            }
        }
        compose.onNodeWithText("AAPL").assertIsDisplayed()
        compose.onNodeWithText("$123.45").assertIsDisplayed()
        compose.onNodeWithText("SIMULATED · USD").assertIsDisplayed()
    }

    @Test
    fun reconnectActionIsAvailableAfterFailure() {
        var retries = 0
        compose.setContent {
            DemoTheme {
                StocksScreen(StocksStateUiModel(isFeedInterrupted = true), onRetry = { retries++ })
            }
        }
        compose.onNodeWithText("Reconnect").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
}
