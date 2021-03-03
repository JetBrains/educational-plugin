package com.jetbrains.edu.learning.stepik.hyperskill.metrics.handlers

import com.jetbrains.edu.learning.Result

interface HyperskillEventsHandler<Event> {
  val pendingEvents: List<Event>

  fun sendEvents(events: List<Event>): Result<List<Event>, String>

  fun addPendingEvents(events: List<Event>)
}