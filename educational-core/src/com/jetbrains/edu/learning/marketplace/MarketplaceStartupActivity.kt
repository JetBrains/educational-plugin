package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.marketplace.update.MarketplaceUpdateChecker
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType.SUBMISSIONS_TAB

class MarketplaceStartupActivity : StartupActivity {

  override fun runActivity(project: Project) {
    if (project.isDisposed || !EduUtils.isStudentProject(project) || isUnitTestMode) return
    val taskManager = StudyTaskManager.getInstance(project)
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return
    if (!course.isMarketplaceRemote) return

    MarketplaceUpdateChecker.getInstance(project).check()

    val submissionsManager = SubmissionsManager.getInstance(project)
    if (!submissionsManager.submissionsSupported()) return

    val account = MarketplaceSettings.INSTANCE.account
    if (account != null && account.isJwtTokenProvided()) {
      submissionsManager.prepareSubmissionsContent { MarketplaceSolutionLoader.getInstance(project).loadSolutionsInBackground() }
    }
    else {
      val busConnection: MessageBusConnection = project.messageBus.connect(taskManager)
      busConnection.subscribe(MarketplaceSubmissionsConnector.GRAZIE_AUTHORIZATION_TOPIC, object : EduLogInListener {
        override fun userLoggedIn() {
          val userAccount = MarketplaceSettings.INSTANCE.account
          if (userAccount == null || userAccount.isJwtTokenProvided()) {
            return
          }
          submissionsManager.prepareSubmissionsContent()
        }

        override fun userLoggedOut() {
          TaskDescriptionView.getInstance(project).updateTab(SUBMISSIONS_TAB)
        }
      })
    }
  }
}