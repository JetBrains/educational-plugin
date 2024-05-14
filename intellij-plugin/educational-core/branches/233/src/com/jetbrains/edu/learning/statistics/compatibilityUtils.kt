package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.*

fun EventLogGroup(
  id: String,
  @Suppress("UNUSED_PARAMETER") description: String,
  version: Int
): EventLogGroup = EventLogGroup(id, version, "FUS")

fun EventLogGroup.registerEvent(
  eventId: String,
  @Suppress("UNUSED_PARAMETER") description: String
): EventId = registerEvent(eventId)

fun <T1> EventLogGroup.registerEvent(
  eventId: String,
  @Suppress("UNUSED_PARAMETER") description: String,
  eventField1: EventField<T1>
): EventId1<T1> = registerEvent(eventId, eventField1)

fun <T1, T2> EventLogGroup.registerEvent(
  eventId: String,
  @Suppress("UNUSED_PARAMETER") description: String,
  eventField1: EventField<T1>,
  eventField2: EventField<T2>
): EventId2<T1, T2> = registerEvent(eventId, eventField1, eventField2)

fun <T1, T2, T3> EventLogGroup.registerEvent(
  eventId: String,
  @Suppress("UNUSED_PARAMETER") description: String,
  eventField1: EventField<T1>,
  eventField2: EventField<T2>,
  eventField3: EventField<T3>
): EventId3<T1, T2, T3> = registerEvent(eventId, eventField1, eventField2, eventField3)

fun EventLogGroup.registerVarargEvent(
  eventId: String,
  @Suppress("UNUSED_PARAMETER") description: String,
  vararg fields: EventField<*>
): VarargEventId = registerVarargEvent(eventId, *fields)
