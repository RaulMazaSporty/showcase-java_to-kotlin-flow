package io.github.rool78.java_to_kotlin_flow.demo

import io.github.rool78.java_to_kotlin_flow.legacy.SimulatedLegacyStockManager
import javax.inject.Inject

interface DemoFeedController {
    fun interruptFeed()
    fun burstUpdates()
}

class SimulatedFeedController @Inject constructor(
    private val manager: SimulatedLegacyStockManager,
) : DemoFeedController {

    override fun interruptFeed() = manager.interruptFeed()
    override fun burstUpdates() = manager.burstUpdates()
}
