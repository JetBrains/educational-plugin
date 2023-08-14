package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.codeforces.CodeforcesUtils.updateCheckStatus
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.update.CodeforcesCourseUpdateChecker
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType

class CodeforcesStartupActivity : StartupActivity {
  override fun runActivity(project: Project) {
    if (project.isDisposed || !project.isStudentProject() || isUnitTestMode) return

    if (project.course !is CodeforcesCourse) return

    val submissionsManager = SubmissionsManager.getInstance(project)

    submissionsManager.prepareSubmissionsContentWhenLoggedIn {
      updateCheckStatus(project)
    }

    project.messageBus.connect().subscribe(CodeforcesSettings.AUTHENTICATION_TOPIC, object : EduLogInListener {
      override fun userLoggedIn() {
        submissionsManager.prepareSubmissionsContentWhenLoggedIn {
          updateCheckStatus(project)
        }
      }

      override fun userLoggedOut() {
        TaskToolWindowView.getInstance(project).updateTab(TabType.SUBMISSIONS_TAB)
      }
    })
    CodeforcesCourseUpdateChecker.getInstance(project).check()
  }
}