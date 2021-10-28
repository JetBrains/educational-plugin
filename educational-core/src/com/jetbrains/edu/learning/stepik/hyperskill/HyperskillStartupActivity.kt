package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.stepik.hyperskill.update.HyperskillCourseUpdateChecker
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType.SUBMISSIONS_TAB
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class HyperskillStartupActivity : StartupActivity {

  override fun runActivity(project: Project) {
    if (project.isDisposed || !EduUtils.isStudentProject(project) || isUnitTestMode) return
    val taskManager = StudyTaskManager.getInstance(project)

    val course = StudyTaskManager.getInstance(project).course as? HyperskillCourse ?: return
    val submissionsManager = SubmissionsManager.getInstance(project)
    if (!submissionsManager.submissionsSupported()) return

    if (HyperskillSettings.INSTANCE.account != null) {
      submissionsManager.prepareSubmissionsContent { HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground() }
    }
    else {
      val busConnection: MessageBusConnection = project.messageBus.connect(taskManager)
      busConnection.subscribe(HyperskillConnector.AUTHORIZATION_TOPIC, object : EduLogInListener {
        override fun userLoggedIn() {
          if (HyperskillSettings.INSTANCE.account == null) {
            return
          }
          submissionsManager.prepareSubmissionsContent()
        }

        override fun userLoggedOut() {
          TaskDescriptionView.getInstance(project).updateTab(SUBMISSIONS_TAB)
        }
      })
    }
    synchronizeTopics(project, course)
    HyperskillCourseUpdateChecker.getInstance(project).check()
  }

  companion object {
    fun synchronizeTopics(project: Project, hyperskillCourse: HyperskillCourse) {
      ApplicationManager.getApplication().executeOnPooledThread {
        HyperskillConnector.getInstance().fillTopics(hyperskillCourse, project)
        YamlFormatSynchronizer.saveRemoteInfo(hyperskillCourse)
      }
    }
  }
}
