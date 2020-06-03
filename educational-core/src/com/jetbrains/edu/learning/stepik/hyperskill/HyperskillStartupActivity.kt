package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.SolutionLoaderBase
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class HyperskillStartupActivity : StartupActivity {

  override fun runActivity(project: Project) {
    if (project.isDisposed || !EduUtils.isStudentProject(project)) return
    val taskManager = StudyTaskManager.getInstance(project)
    project.messageBus.connect(taskManager)
      .subscribe(HyperskillSolutionLoader.SOLUTION_TOPIC, object : SolutionLoaderBase.SolutionLoadingListener {
        override fun solutionLoaded(course: Course) {
          if (course is HyperskillCourse) {
            HyperskillCourseUpdateChecker(project, course, taskManager).check()
          }
        }
      })
    synchronizeHyperskillProject(project)
  }

  companion object {
    @JvmStatic
    fun synchronizeHyperskillProject(project: Project) {
      ApplicationManager.getApplication().executeOnPooledThread {
        val hyperskillCourse = StudyTaskManager.getInstance(project).course as? HyperskillCourse
        if (hyperskillCourse != null) {
          HyperskillConnector.getInstance().fillTopics(hyperskillCourse, project)
          YamlFormatSynchronizer.saveRemoteInfo(hyperskillCourse)
        }
      }
      if (HyperskillSettings.INSTANCE.account != null) {
        HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground()
      }
    }
  }
}
