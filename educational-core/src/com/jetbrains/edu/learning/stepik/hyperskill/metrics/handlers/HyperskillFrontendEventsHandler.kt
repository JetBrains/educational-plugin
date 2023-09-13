package com.jetbrains.edu.learning.stepik.hyperskill.metrics.handlers

import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillFrontendEvent
import com.jetbrains.edu.learning.stepik.hyperskill.metrics.HyperskillMetricsService

object HyperskillFrontendEventsHandler : HyperskillEventsHandler<HyperskillFrontendEvent> {
  override val pendingEvents: List<HyperskillFrontendEvent>
    get() = HyperskillMetricsService.getInstance().allFrontendEvents(true)

  override fun sendEvents(events: List<HyperskillFrontendEvent>): Result<Any, String> {
    return HyperskillConnector.getInstance().sendFrontendEvents(events)
  }

  override fun addPendingEvents(events: List<HyperskillFrontendEvent>) {
    HyperskillMetricsService.getInstance().addAllFrontendEvents(events)
  }
}