package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.codeforces.submissions.CodeforcesSolutionLoader
import com.jetbrains.edu.learning.codeforces.update.CodeforcesCourseUpdateChecker
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType

class CodeforcesStartupActivity : StartupActivity {
  override fun runActivity(project: Project) {
    if (project.isDisposed || !EduUtils.isStudentProject(project) || isUnitTestMode) return

    val submissionsManager = SubmissionsManager.getInstance(project)

    if (CodeforcesSettings.getInstance().isLoggedIn()) {
      submissionsManager.prepareSubmissionsContent {
        CodeforcesSolutionLoader.getInstance(project).loadSolutionsInBackground()
      }
    }
    else {
      project.messageBus.connect().subscribe(CodeforcesSettings.AUTHENTICATION_TOPIC, object : EduLogInListener {
        override fun userLoggedIn() {
          if (CodeforcesSettings.getInstance().isLoggedIn()) {
            submissionsManager.prepareSubmissionsContent()
          }
        }

        override fun userLoggedOut() {
          TaskDescriptionView.getInstance(project).updateTab(TabType.SUBMISSIONS_TAB)
        }
      })
    }
    CodeforcesCourseUpdateChecker.getInstance(project).check()
  }
}