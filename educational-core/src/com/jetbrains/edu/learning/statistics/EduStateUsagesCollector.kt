package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventField
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.JavaUILibrary
import java.util.*
import kotlin.collections.HashSet

/**
 * IMPORTANT: if you modify anything in this class, updated whitelist rules should be
 * provided to analytics platform team.
 */
@Suppress("UnstableApiUsage")
class EduStateUsagesCollector : ApplicationUsagesCollector() {

  private enum class EduRole {
    STUDENT, EDUCATOR
  }

  override fun getGroup(): EventLogGroup = GROUP

  override fun getMetrics(): Set<MetricEvent> {
    val metrics = HashSet<MetricEvent>()

    val taskPanel = EduSettings.getInstance().javaUiLibrary
    metrics += TASK_PANEL_EVENT.metric(taskPanel)

    val role = if (CCPluginToggleAction.isCourseCreatorFeaturesEnabled) EduRole.EDUCATOR else EduRole.STUDENT
    metrics += ROLE_EVENT.metric(role)

    return metrics
  }

  companion object {
    private val GROUP = EventLogGroup("educational.state", 2)

    private val TASK_PANEL_EVENT = GROUP.registerEvent("task.panel", enumField<JavaUILibrary>())
    private val ROLE_EVENT = GROUP.registerEvent("role", enumField<EduRole>())

    private inline fun <reified T : Enum<*>> enumField() : EventField<T> {
      // field name and transform function are written in this way to be compatible with previous version
      return EventFields.Enum("value") { it.name.toLowerCase(Locale.ENGLISH) }
    }
  }
}
