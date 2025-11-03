package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.welcomeScreen.JBACourseFromStorage
import com.jetbrains.edu.learning.statistics.EduFields.COURSE_ID_FIELD
import com.jetbrains.edu.learning.statistics.EduFields.COURSE_MODE_FIELD
import com.jetbrains.edu.learning.statistics.EduFields.LANGUAGE_FIELD
import com.jetbrains.edu.learning.statistics.EduFields.PLATFORM_FIELD

/**
 * IMPORTANT: if you modify anything in this class, updated whitelist rules should be
 * provided to analytics platform team.
 */
@Suppress("UnstableApiUsage")
class EduStateUsagesCollector : ApplicationUsagesCollector() {

  override fun getGroup(): EventLogGroup = GROUP

  override fun getMetrics(): Set<MetricEvent> {
    val metrics = HashSet<MetricEvent>()

    val taskPanel = EduSettings.getInstance().javaUiLibrary
    metrics += TASK_PANEL_EVENT.metric(taskPanel)

    CoursesStorage.getInstance().getAllCourses()
      .filterIsInstance<JBACourseFromStorage>()
      .forEach { course ->
        metrics += COURSE_EVENT.metric(
          COURSE_ID_FIELD.with(course.id),
          COURSE_MODE_FIELD.with(course.courseMode),
          PLATFORM_FIELD.with(course.itemType),
          LANGUAGE_FIELD.with(course.languageId)
        )
      }

    return metrics
  }

  companion object {
    private val GROUP = EventLogGroup(
      "educational.state",
      "The metric is reported in case a user has JetBrains Academy plugin installed and contains data about settings related to the plugin.",
      8
    )

    private val TASK_PANEL_EVENT = GROUP.registerEvent(
      "task.panel",
      "Task Panel",
      enumField<JavaUILibrary>()
    )

    private val COURSE_EVENT = GROUP.registerVarargEvent(
      "course",
      "The metric shows base info about educational course opened in IDE",
      COURSE_ID_FIELD,
      COURSE_MODE_FIELD,
      PLATFORM_FIELD,
      LANGUAGE_FIELD
    )
  }
}
