package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CPP
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.OBJECTIVE_C
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.welcomeScreen.JBACourseFromStorage
import com.jetbrains.edu.learning.statistics.EduFields.COURSE_ID_FIELD
import com.jetbrains.edu.learning.statistics.EduFields.COURSE_MODE_FIELD
import com.jetbrains.edu.learning.statistics.EduFields.LANGUAGE_FIELD
import com.jetbrains.edu.learning.statistics.EduFields.PLATFORM_FIELD

/**
 * IMPORTANT: if you modify anything in this class, remember to increment [GROUP] version.
 *
 * Whitelist rule scheme is automatically generated on CI,
 * and for any changes a new verification issue will be created in https://youtrack.jetbrains.com/projects/FUS
 * together with a merge request with the corresponding changes in the scheme.
 *
 * See:
 * - [Event scheme generator](https://buildserver.labs.intellij.net/buildConfiguration/ijplatform_master_EduStatisticsEventSchemeGenerator)
 * - [Event scheme changes calculator](https://buildserver.labs.intellij.net/buildConfiguration/FUS_FusWhitelist_EventScheme_EduToolsChangesCalculation)
 */
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
          LANGUAGE_FIELD.with(course.languageIdForCollectors)
        )
      }

    return metrics
  }

  @Suppress("CompanionObjectInExtension")
  companion object {
    private val GROUP = EventLogGroup("educational.state", 8)

    private val TASK_PANEL_EVENT = GROUP.registerEvent(
      "task.panel",
      enumField<JavaUILibrary>()
    )

    private val COURSE_EVENT = GROUP.registerVarargEvent(
      "course",
      COURSE_ID_FIELD,
      COURSE_MODE_FIELD,
      PLATFORM_FIELD,
      LANGUAGE_FIELD
    )

    private val JBACourseFromStorage.languageIdForCollectors: String
      get() = if (languageId == CPP) {
        OBJECTIVE_C
      }
      else {
        languageId
      }
  }
}
