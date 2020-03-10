package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.jetbrains.edu.learning.EduUtils.isStudentProject
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.SolutionLoaderBase
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader.Companion.IS_HYPERSKILL_SOLUTION_LOADING_STARTED
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

@Suppress("ComponentNotRegistered") // Hyperskill.xml
class HyperskillProjectComponent(private val project: Project) : ProjectComponent, SolutionLoaderBase.SolutionLoadingListener {
  override fun projectOpened() {
    if (project.isDisposed || !isStudentProject(project)) return

    StartupManager.getInstance(project).runWhenProjectIsInitialized {
      project.messageBus.connect().subscribe(HyperskillSolutionLoader.SOLUTION_TOPIC, this)
      synchronizeHyperskillProject(project)
    }
  }

  override fun solutionLoaded(course: Course) {
    if (course is HyperskillCourse) {
      HyperskillCourseUpdater.updateCourse(project, course)
    }
  }

  companion object {
    fun synchronizeHyperskillProject(project: Project) {
      val course = StudyTaskManager.getInstance(project).course as? HyperskillCourse ?: return
      if (course.taskToTopics.isEmpty()) {
        ApplicationManager.getApplication().executeOnPooledThread {
          HyperskillConnector.getInstance().fillTopics(course, project)
          YamlFormatSynchronizer.saveRemoteInfo(course)
        }
      }
      val isSolutionLoadingStarted = IS_HYPERSKILL_SOLUTION_LOADING_STARTED.getRequired(course)
      if (HyperskillSettings.INSTANCE.account != null && !isSolutionLoadingStarted) {
        HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground()
      }
    }
  }
}
