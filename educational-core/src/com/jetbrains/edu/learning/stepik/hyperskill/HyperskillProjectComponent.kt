package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.jetbrains.edu.learning.EduUtils.isStudyProject
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.SolutionLoaderBase
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillSolutionLoader.Companion.IS_HYPERSKILL_SOLUTION_LOADING_STARTED
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class HyperskillProjectComponent(private val project: Project) : ProjectComponent, SolutionLoaderBase.SolutionLoadingListener {
  override fun projectOpened() {
    if (project.isDisposed || !isStudyProject(project)) return

    StartupManager.getInstance(project).runWhenProjectIsInitialized {
      project.messageBus.connect().subscribe(HyperskillSolutionLoader.SOLUTION_TOPIC, this)

      val course = StudyTaskManager.getInstance(project).course as? HyperskillCourse ?: return@runWhenProjectIsInitialized
      if (course.taskToTopics.isEmpty()) {
        HyperskillConnector.getInstance().fillTopics(course, project)
      }
      val isSolutionLoadingStarted = IS_HYPERSKILL_SOLUTION_LOADING_STARTED.getRequired(course)
      if (HyperskillSettings.INSTANCE.account != null && !isSolutionLoadingStarted) {
        HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground()
      }
    }
  }

  override fun solutionLoaded(course: Course) {
    if (course is HyperskillCourse) {
      HyperskillCourseUpdater.updateCourse(project, course)
    }
  }
}
