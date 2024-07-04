package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventField
import com.intellij.internal.statistic.eventLog.events.EventId1
import com.intellij.internal.statistic.eventLog.events.EventId2
import com.intellij.internal.statistic.eventLog.events.EventId3

// BACKCOMPAT: 2023.3. Move to `com/jetbrains/edu/learning/statistics/utils.kt`
fun EventLogGroup(
  id: String,
  description: String,
  version: Int
): EventLogGroup = EventLogGroup(id, version, "FUS", description)

// BACKCOMPAT: 2023.3. Move to `com/jetbrains/edu/learning/statistics/utils.kt`
fun <T1> EventLogGroup.registerEvent(
  eventId: String,
  description: String,
  eventField1: EventField<T1>
): EventId1<T1> = registerEvent(eventId, eventField1, description)

// BACKCOMPAT: 2023.3. Move to `com/jetbrains/edu/learning/statistics/utils.kt`
fun <T1, T2> EventLogGroup.registerEvent(
  eventId: String,
  description: String,
  eventField1: EventField<T1>,
  eventField2: EventField<T2>
): EventId2<T1, T2> = registerEvent(eventId, eventField1, eventField2, description)

// BACKCOMPAT: 2023.3. Move to `com/jetbrains/edu/learning/statistics/utils.kt`
fun <T1, T2, T3> EventLogGroup.registerEvent(
  eventId: String,
  description: String,
  eventField1: EventField<T1>,
  eventField2: EventField<T2>,
  eventField3: EventField<T3>
): EventId3<T1, T2, T3> = registerEvent(eventId, eventField1, eventField2, eventField3, description)
