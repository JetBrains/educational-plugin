package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.eventLog.events.EventField
import com.intellij.internal.statistic.eventLog.events.EventFields
import java.util.*

@Suppress("UnstableApiUsage")
inline fun <reified T : Enum<*>> enumField(name: String = "value") : EventField<T> {
  return EventFields.Enum(name) { it.toString().lowercase(Locale.ENGLISH) }
}
