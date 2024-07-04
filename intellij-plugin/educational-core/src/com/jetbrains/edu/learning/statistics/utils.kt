package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.*
import java.util.*

@Suppress("UnstableApiUsage")
inline fun <reified T : Enum<*>> enumField(name: String = "value") : EventField<T> {
  return EventFields.Enum(name) { it.toString().lowercase(Locale.ENGLISH) }
}

fun EventLogGroup(
  id: String,
  description: String,
  version: Int
): EventLogGroup = EventLogGroup(id, version, "FUS", description)

fun <T1> EventLogGroup.registerEvent(
  eventId: String,
  description: String,
  eventField1: EventField<T1>
): EventId1<T1> = registerEvent(eventId, eventField1, description)

fun <T1, T2> EventLogGroup.registerEvent(
  eventId: String,
  description: String,
  eventField1: EventField<T1>,
  eventField2: EventField<T2>
): EventId2<T1, T2> = registerEvent(eventId, eventField1, eventField2, description)

fun <T1, T2, T3> EventLogGroup.registerEvent(
  eventId: String,
  description: String,
  eventField1: EventField<T1>,
  eventField2: EventField<T2>,
  eventField3: EventField<T3>
): EventId3<T1, T2, T3> = registerEvent(eventId, eventField1, eventField2, eventField3, description)
