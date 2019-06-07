package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.beans.UsageDescriptor
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.intellij.internal.statistic.utils.getEnumUsage
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.learning.EduSettings

class EduStateUsagesCollector : ApplicationUsagesCollector() {
  override fun getUsages(): MutableSet<UsageDescriptor> {
    val descriptors = HashSet<UsageDescriptor>()

    val taskPanel =
      if (EduSettings.getInstance().shouldUseJavaFx()) TaskDescriptionPanel.JAVAFX else TaskDescriptionPanel.SWING
    descriptors.add(getEnumUsage("task.panel", taskPanel))

    val role = if (CCPluginToggleAction.isCourseCreatorFeaturesEnabled) EduRole.EDUCATOR else EduRole.STUDENT
    descriptors.add(getEnumUsage("role", role))

    return descriptors
  }

  private enum class TaskDescriptionPanel {
    SWING, JAVAFX
  }

  private enum class EduRole {
    STUDENT, EDUCATOR
  }

  override fun getGroupId() = "educational.state"

  override fun getVersion() = 1
}