package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.jetbrains.edu.learning.courseFormat.StudyItem

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

  @JvmStatic
  fun studyItemCreated(item: StudyItem) {
    val data = FeatureUsageData()
    data.addData(MODE, item.course.courseMode)
    data.addData("type", item.itemType)
    FUCounterUsageLogger.getInstance().logEvent(GROUP_ID, "study.item.created", data)
  }

  private const val GROUP_ID = "educational.counters"
  private const val MODE = "mode"
}