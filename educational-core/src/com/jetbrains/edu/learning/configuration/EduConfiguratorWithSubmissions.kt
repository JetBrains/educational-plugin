package com.jetbrains.edu.learning.configuration

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.StepikAuthorizer
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikSubmissionsManager
import javax.swing.JPanel

abstract class EduConfiguratorWithSubmissions<Settings> : EduConfigurator<Settings> {

  override fun additionalTaskTabs(currentTask: Task?, project: Project): List<Pair<JPanel, String>> {
    val submissionsTab = createSubmissionsTab(currentTask, project,
                                              StepikSubmissionsManager,
                                              "Stepik.org",
                                              EduSettings.isLoggedIn()) { doAuthorize() } ?: return emptyList()
    return listOf(submissionsTab)
  }

  override fun submissionsTab(currentTask: Task?, project: Project): Pair<JPanel, String>? {
    return createSubmissionsTab(currentTask, project, StepikSubmissionsManager, "Stepik.org",
                                EduSettings.isLoggedIn()) { doAuthorize() }
  }

  private fun doAuthorize() {
    StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
    EduCounterUsageCollector.loggedIn(StepikNames.STEPIK, EduCounterUsageCollector.AuthorizationPlace.SUBMISSIONS_TAB)
  }
}