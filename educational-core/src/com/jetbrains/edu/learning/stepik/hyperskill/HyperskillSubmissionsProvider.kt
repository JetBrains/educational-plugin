package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.stepik.submissions.SubmissionsProvider

class HyperskillSubmissionsProvider : SubmissionsProvider {

  override fun loadAllSubmissions(project: Project, course: Course): Map<Int, MutableList<Submission>> {
    if (!submissionsCanBeShown(course) || !isLoggedIn()) return emptyMap()
    val stepIds = HyperskillSolutionLoader.getInstance(project).provideTasksToUpdate(course).map { it.id }.toSet()
    return loadSubmissions(stepIds)
  }

  override fun loadSubmissions(stepIds: Set<Int>): Map<Int, MutableList<Submission>> {
    val submissionsById = mutableMapOf<Int, MutableList<Submission>>()
    val submissionsList = HyperskillConnector.getInstance().getSubmissions(stepIds)
    return submissionsList.groupByTo(submissionsById) { it.step }
  }

  override fun submissionsCanBeShown(course: Course): Boolean {
    return course is HyperskillCourse && course.isStudy
  }

  override fun getPlatformName(): String = EduNames.JBA

  override fun isLoggedIn(): Boolean = HyperskillSettings.INSTANCE.account != null

  override fun doAuthorize() {
    HyperskillConnector.getInstance().doAuthorize()
    EduCounterUsageCollector.loggedIn(HYPERSKILL, EduCounterUsageCollector.AuthorizationPlace.SUBMISSIONS_TAB)
  }
}