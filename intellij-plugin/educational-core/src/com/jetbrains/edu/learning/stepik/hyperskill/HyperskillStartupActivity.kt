package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.ide.util.RunOnceUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
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

    showNewHyperskillPluginInfoNotification(project)
  }

  private fun showNewHyperskillPluginInfoNotification(project: Project) {
    RunOnceUtil.runOnceForApp(NEW_HYPERSKILL_PLUGIN_NOTIFICATION_KEY) {
      EduNotificationManager.create(
        type = NotificationType.WARNING,
        title = EduCoreBundle.message("hyperskill.new.plugin.notification.title"),
        content = EduCoreBundle.message("hyperskill.new.plugin.notification.text"),
      ).addAction(object : AnAction(EduCoreBundle.message("hyperskill.new.plugin.notification.action.text")) {
        override fun actionPerformed(e: AnActionEvent) {
          EduBrowser.getInstance().browse(NEW_HYPERSKILL_PLUGIN_INFO_LINK)
          e.getData(Notification.KEY)?.expire()
        }
      }).notify(project)
    }
  }

  companion object {
    private const val NEW_HYPERSKILL_PLUGIN_NOTIFICATION_KEY = "edu.new.hyperskill.plugin.notification"
    private const val NEW_HYPERSKILL_PLUGIN_INFO_LINK = "https://jb.gg/academy/hyperskill/plugin"

    fun synchronizeTopics(project: Project, hyperskillCourse: HyperskillCourse) {
      ApplicationManager.getApplication().executeOnPooledThread {
        HyperskillConnector.getInstance().fillTopics(project, hyperskillCourse)
        YamlFormatSynchronizer.saveRemoteInfo(hyperskillCourse)
      }
    }
  }
}
