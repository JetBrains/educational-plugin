package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger

fun reportEvent(eventId: String, additionalData: Map<String, String> = emptyMap()) {
  val data = FeatureUsageData()
  additionalData.forEach {
    data.addData(it.key, it.value)
  }
  FUCounterUsageLogger.getInstance().logEvent(EduCounterUsageCollector.GROUP_ID, eventId, data)
}