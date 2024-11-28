package com.jetbrains.edu.learning.marketplace.submissions

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.authUtils.AuthorizationPlace
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.JET_BRAINS_ACCOUNT
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceStateOnClose
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.submissions.SubmissionSettings
import com.jetbrains.edu.learning.submissions.provider.SubmissionsData
import com.jetbrains.edu.learning.submissions.provider.SubmissionsProvider

class MarketplaceSubmissionsProvider : SubmissionsProvider {

  override fun loadAllSubmissions(course: Course): SubmissionsData {
    if (course is EduCourse && course.isMarketplaceRemote) {
      return loadSubmissions(course.allTasks, course.id)
    }
    return emptyMap()
  }

  override fun loadCourseStateOnClose(project: Project, course: Course): Map<Int, MarketplaceStateOnClose> {
    if (SubmissionSettings.getInstance(project).stateOnClose && course is EduCourse && course.isMarketplaceRemote) {
      val states = MarketplaceSubmissionsConnector.getInstance().getCourseStateOnClose(course.id)
      return states.associateBy { it.taskId }
    }
    return emptyMap()
  }

  override fun loadSubmissions(tasks: List<Task>, courseId: Int): SubmissionsData {
    val submissions = MarketplaceSubmissionsConnector.getInstance().getAllSubmissions(courseId)
    val submissionsById = mutableMapOf<Int, MutableList<MarketplaceSubmission>>()
    return submissions.groupByTo(submissionsById) { it.taskId }
  }

  override fun loadSolutionFiles(submission: MarketplaceSubmission) {
    if (submission.solutionKey.isNotBlank()) {
      submission.solutionFiles = MarketplaceSubmissionsConnector.getInstance().loadSolutionFiles(submission.solutionKey)
    }
  }

  override fun areSubmissionsAvailable(course: Course): Boolean {
    return course is EduCourse && course.isStudy && course.isMarketplaceRemote
  }

  override fun isLoggedIn(): Boolean {
    return MarketplaceConnector.getInstance().isLoggedIn()
  }

  override fun getPlatformName(): String = JET_BRAINS_ACCOUNT

  override fun doAuthorize(vararg postLoginActions: Runnable) {
    MarketplaceConnector.getInstance().doAuthorize(*postLoginActions, authorizationPlace = AuthorizationPlace.SUBMISSIONS_TAB)
  }
}