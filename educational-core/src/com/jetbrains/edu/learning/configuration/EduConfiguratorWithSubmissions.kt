package com.jetbrains.edu.learning.configuration

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikSubmissionsManager
import javax.swing.JPanel

abstract class EduConfiguratorWithSubmissions<Settings> : EduConfigurator<Settings> {

  override fun additionalTaskTabs(currentTask: Task?, project: Project): List<Pair<JPanel, String>> {
    val submissionsTab = createSubmissionsTab(currentTask, project,
                                              StepikSubmissionsManager) ?: return emptyList()
    return listOf(submissionsTab)
  }
}