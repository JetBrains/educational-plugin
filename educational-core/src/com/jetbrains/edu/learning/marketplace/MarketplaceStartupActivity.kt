package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.update.MarketplaceUpdateChecker
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType.SUBMISSIONS_TAB

class MarketplaceStartupActivity : StartupActivity {

  override fun runActivity(project: Project) {
    if (project.isDisposed || !EduUtils.isStudentProject(project) || isUnitTestMode) return
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return
    if (!course.isMarketplaceRemote) return

    MarketplaceUpdateChecker.getInstance(project).check()

    val submissionsManager = SubmissionsManager.getInstance(project)
    if (!submissionsManager.submissionsSupported()) return

    val marketplaceConnector = MarketplaceConnector.getInstance()

    if (marketplaceConnector.isLoggedIn()) {
      submissionsManager.prepareSubmissionsContent { MarketplaceSolutionLoader.getInstance(project).loadSolutionsInForeground() }
    }

    MarketplaceConnector.getInstance().setSubmissionTabListener(object : EduLogInListener {
      override fun userLoggedIn() {
        if (project.isDisposed) return
        submissionsManager.prepareSubmissionsContent()
      }

      override fun userLoggedOut() {
        if (project.isDisposed) return
        TaskDescriptionView.getInstance(project).updateTab(SUBMISSIONS_TAB)
      }
    })
  }
}