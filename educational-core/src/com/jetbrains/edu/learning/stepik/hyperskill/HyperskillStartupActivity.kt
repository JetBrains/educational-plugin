package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.SolutionLoaderBase
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.stepik.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class HyperskillStartupActivity : StartupActivity {

  override fun runActivity(project: Project) {
    if (project.isDisposed || !EduUtils.isStudentProject(project) || !isUnitTestMode) return
    val taskManager = StudyTaskManager.getInstance(project)
    project.messageBus.connect(taskManager)
      .subscribe(HyperskillSolutionLoader.SOLUTION_TOPIC, object : SolutionLoaderBase.SolutionLoadingListener {
        override fun solutionLoaded(course: Course) {
          if (course is HyperskillCourse) {
            HyperskillCourseUpdateChecker(project, course, taskManager).check()
          }
        }
      })

    val course = StudyTaskManager.getInstance(project).course
    val submissionsManager = SubmissionsManager.getInstance(project)
    if (course is HyperskillCourse && submissionsManager.submissionsSupported()) {
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
            TaskDescriptionView.getInstance(project).updateSubmissionsTab()
          }
        })
      }
      synchronizeTopics(project, course)
    }
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
