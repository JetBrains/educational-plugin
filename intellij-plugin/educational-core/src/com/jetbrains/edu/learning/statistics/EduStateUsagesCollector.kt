package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.JavaUILibrary

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

    return metrics
  }

  companion object {
    private val GROUP = EventLogGroup(
      "educational.state",
      "The metric is reported in case a user has JetBrains Academy plugin installed and contains data about settings related to the plugin.",
      4
    )

    private val TASK_PANEL_EVENT = GROUP.registerEvent(
      "task.panel",
      "Task Panel",
      enumField<JavaUILibrary>()
    )
  }
}
