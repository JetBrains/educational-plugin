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

  @JvmStatic
  fun taskNavigation(place: TaskNavigationPlace) {
    FUCounterUsageLogger.getInstance().logEvent(GROUP_ID, "navigate.to.task", FeatureUsageData().addData("source", place.toString()))
  }

  @JvmStatic
  fun eduProjectCreated(mode: String) {
    FUCounterUsageLogger.getInstance().logEvent(GROUP_ID, "edu.project.created", FeatureUsageData().addData(MODE, mode))
  }

  @JvmStatic
  fun eduProjectOpened(mode: String) {
    FUCounterUsageLogger.getInstance().logEvent(GROUP_ID, "edu.project.opened", FeatureUsageData().addData(MODE, mode))
  }

  private const val GROUP_ID = "educational.counters"
  private const val MODE = "mode"
}