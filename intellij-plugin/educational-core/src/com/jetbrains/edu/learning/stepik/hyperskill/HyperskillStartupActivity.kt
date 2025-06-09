package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.update.HyperskillCourseUpdateChecker
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType.SUBMISSIONS_TAB
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class HyperskillStartupActivity : StartupActivity {

  override fun runActivity(project: Project) {
    if (project.isDisposed || !project.isStudentProject() || isUnitTestMode) return

    val course = StudyTaskManager.getInstance(project).course as? HyperskillCourse ?: return
    val submissionsManager = SubmissionsManager.getInstance(project)
    if (!submissionsManager.submissionsSupported()) return

    submissionsManager.prepareSubmissionsContentWhenLoggedIn {
      HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground()
    }

    HyperskillConnector.getInstance().setSubmissionTabListener(object : EduLogInListener {
      override fun userLoggedIn() {
        if (project.isDisposed) return
        submissionsManager.prepareSubmissionsContentWhenLoggedIn()
      }

      override fun userLoggedOut() {
        if (project.isDisposed) return
        TaskToolWindowView.getInstance(project).updateTab(SUBMISSIONS_TAB)
      }
    })

    synchronizeTopics(project, course)
    HyperskillCourseUpdateChecker.getInstance(project).check()
  }

  companion object {
    fun synchronizeTopics(project: Project, hyperskillCourse: HyperskillCourse) {
      ApplicationManager.getApplication().executeOnPooledThread {
        HyperskillConnector.getInstance().fillTopics(project, hyperskillCourse)
        YamlFormatSynchronizer.saveRemoteInfo(hyperskillCourse)
      }
    }
  }
}
