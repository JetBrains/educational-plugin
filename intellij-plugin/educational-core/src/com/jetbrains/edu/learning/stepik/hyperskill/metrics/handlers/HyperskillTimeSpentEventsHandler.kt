package com.jetbrains.edu.learning.stepik.hyperskill.metrics.handlers

import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillTimeSpentEvent
import com.jetbrains.edu.learning.stepik.hyperskill.metrics.HyperskillMetricsService

object HyperskillTimeSpentEventsHandler : HyperskillEventsHandler<HyperskillTimeSpentEvent> {
  override val pendingEvents: List<HyperskillTimeSpentEvent>
    get() = HyperskillMetricsService.getInstance().allTimeSpentEvents(reset = true)

  override fun sendEvents(events: List<HyperskillTimeSpentEvent>): Result<Any, String> =
    HyperskillConnector.getInstance().sendTimeSpentEvents(events)

  override fun addPendingEvents(events: List<HyperskillTimeSpentEvent>) {
    HyperskillMetricsService.getInstance().addAllTimeSpentEvents(events.associateBy({ it.step }, { it.duration }))
  }
}