package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.authUtils.AuthorizationPlace
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.submissions.provider.SubmissionsData
import com.jetbrains.edu.learning.submissions.provider.SubmissionsProvider

class HyperskillSubmissionsProvider : SubmissionsProvider {

  override fun loadAllSubmissions(course: Course): SubmissionsData {
    if (!areSubmissionsAvailable(course) || !isLoggedIn()) return emptyMap()
    return loadSubmissions(course.allTasks, course.id)
  }

  override fun loadSubmissions(tasks: List<Task>, courseId: Int): SubmissionsData {
    val stepIds = tasks.map { it.id }.toSet()
    val submissionsById = mutableMapOf<Int, MutableList<StepikBasedSubmission>>()
    val submissionsList = HyperskillConnector.getInstance().getSubmissions(stepIds)
    return submissionsList.groupByTo(submissionsById) { it.taskId }
  }

  override fun areSubmissionsAvailable(course: Course): Boolean {
    return course is HyperskillCourse && course.isStudy
  }

  override fun getPlatformName(): String = EduNames.JBA

  override fun isLoggedIn(): Boolean = HyperskillSettings.INSTANCE.account != null

  override fun doAuthorize(vararg postLoginActions: Runnable) {
    HyperskillConnector.getInstance().doAuthorize(authorizationPlace = AuthorizationPlace.SUBMISSIONS_TAB)
  }
}