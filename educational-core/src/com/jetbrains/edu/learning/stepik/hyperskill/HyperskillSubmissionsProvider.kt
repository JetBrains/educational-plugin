package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.SubmissionsManager
import com.jetbrains.edu.learning.stepik.SubmissionsProvider
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView

class HyperskillSubmissionsProvider : SubmissionsProvider() {

  override fun loadAllSubmissions(project: Project, course: Course?, onFinish: () -> Unit) {
    if (!submissionsCanBeShown(course) || !isLoggedIn()) return
    ApplicationManager.getApplication().executeOnPooledThread {
      val stepIds = HyperskillSolutionLoader.getInstance(project).provideTasksToUpdate(course!!).map { it.id }.toSet()
      SubmissionsManager.getInstance(project).getSubmissions(course, stepIds)
      onFinish()
      ApplicationManager.getApplication().invokeLater { TaskDescriptionView.getInstance(project).updateSubmissionsTab() }
    }
  }

  override fun loadSubmissions(stepId: Int): List<Submission> {
    return HyperskillConnector.getInstance().getSubmissions(setOf(stepId))
  }

  override fun submissionsCanBeShown(course: Course?): Boolean {
    return course is HyperskillCourse && course.isStudy
  }

  override fun getPlatformName(): String = EduNames.JBA

  override fun isLoggedIn(): Boolean = HyperskillSettings.INSTANCE.account != null

  override fun doAuthorize() {
    HyperskillConnector.getInstance().doAuthorize()
    EduCounterUsageCollector.loggedIn(HYPERSKILL, EduCounterUsageCollector.AuthorizationPlace.SUBMISSIONS_TAB)
  }
}