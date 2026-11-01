package io.github.rool78.java_to_kotlin_flow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class StocksAppTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun javaCallbacksReachTheComposeScreen() {
        compose.onNodeWithTag("price_AAPL").assertIsDisplayed()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Awaiting price").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag("watchlist").performScrollToNode(hasTestTag("price_MSFT"))
        compose.onNodeWithTag("price_MSFT").assertIsDisplayed()
        compose.onNodeWithTag("watchlist").performScrollToNode(hasTestTag("price_GOOGL"))
        compose.onNodeWithTag("price_GOOGL").assertIsDisplayed()
    }

    @Test
    fun interruptionAndReconnectUseTheRealJavaManager() {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Awaiting price").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("Interrupt feed").performScrollTo().performClick()
        compose.onNodeWithText("Live prices interrupted. Try reconnecting.").assertIsDisplayed()
        compose.onNodeWithText("Interrupt feed").assertIsNotEnabled()
        compose.onNodeWithText("Reconnect").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Awaiting price").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithText("Reconnect").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun selectorAddsAnotherCompany() {
        compose.onNodeWithTag("stock_option_NVDA").performClick().assertIsSelected()
        compose.onNodeWithTag("watchlist").performScrollToNode(hasTestTag("price_NVDA"))
        compose.onNodeWithTag("price_NVDA").assertIsDisplayed()
        compose.onNodeWithText("NVIDIA").assertIsDisplayed()
    }

    @Test fun slowProcessingAndBurstInterruptTheFeed() {
        compose.onNodeWithTag("slow_processing").performScrollTo().performClick().assertIsOn()
        compose.onNodeWithText("Burst updates").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("Reconnect").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Burst updates").assertIsNotEnabled()
        compose.onNodeWithTag("slow_processing").assertIsOn()
        compose.onNodeWithText("Reconnect").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Awaiting price").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithText("Reconnect").fetchSemanticsNodes().isEmpty()
        }
    }
}
