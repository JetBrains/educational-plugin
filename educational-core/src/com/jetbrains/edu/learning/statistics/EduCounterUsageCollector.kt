package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger

object EduCounterUsageCollector {
  enum class TaskNavigationPlace {
    CHECK_ALL_NOTIFICATION,
    TASK_DESCRIPTION_TOOLBAR,
    CHECK_PANEL,
    UNRESOLVED_DEPENDENCY_NOTIFICATION
  }

  fun taskNavigation(place: TaskNavigationPlace) {
    FUCounterUsageLogger.getInstance().logEvent(GROUP_ID, "navigate.to.task", FeatureUsageData().addData("source", place.toString()))
  }

  private const val GROUP_ID = "educational.counters"
}