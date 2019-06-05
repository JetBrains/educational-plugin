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
    FUCounterUsageLogger.getInstance().logEvent(GROUP_ID, "navigate.to.task", FeatureUsageData().addData(SOURCE, place.toString()))
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

  enum class LinkType {
    IN_COURSE, STEPIK, EXTERNAL, PSI
  }

  @JvmStatic
  fun linkClicked(linkType: LinkType) {
    FUCounterUsageLogger.getInstance().logEvent(GROUP_ID, "link.clicked", FeatureUsageData().addData("linkType", linkType.toString()))
  }

  private enum class AuthorizationEvent {
    LOG_IN, LOG_OUT
  }

  enum class AuthorizationPlace {
    SETTINGS, WIDGET, START_COURSE_DIALOG
  }

  private fun authorization(event: AuthorizationEvent, platform: String, place: AuthorizationPlace) {
    val data = FeatureUsageData()
    data.addData("event", event.toString())
    data.addData("platform", platform)
    data.addData(SOURCE, place.toString())
    FUCounterUsageLogger.getInstance().logEvent(GROUP_ID, "authorization", data)
  }

  @JvmStatic
  fun loggedIn(platform: String, place: AuthorizationPlace) {
    authorization(EduCounterUsageCollector.AuthorizationEvent.LOG_IN, platform, place)
  }

  @JvmStatic
  fun loggedOut(platform: String, place: AuthorizationPlace) {
    authorization(EduCounterUsageCollector.AuthorizationEvent.LOG_OUT, platform, place)
  }

  private const val GROUP_ID = "educational.counters"
  private const val MODE = "mode"
  private const val SOURCE = "source"
}