package com.jetbrains.edu.learning.marketplace

import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.edu.learning.authUtils.AuthorizationPlace
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.submissions.SubmissionsProvider
import com.jetbrains.edu.learning.submissions.isSubmissionDownloadAllowed
import com.jetbrains.edu.learning.submissions.isSolutionSharingAllowed

class MarketplaceSubmissionsProvider : SubmissionsProvider {

  override fun loadAllSubmissions(course: Course): Map<Int, List<MarketplaceSubmission>> {
    if (course is EduCourse && course.isMarketplaceRemote) {
      return loadSubmissions(course.allTasks, course.id)
    }
    return emptyMap()
  }

  override fun loadSharedSolutionsForCourse(course: Course): Map<Int, List<MarketplaceSubmission>> {
    if (course !is EduCourse || !course.isMarketplace) return emptyMap()

    val submissionsConnector = MarketplaceSubmissionsConnector.getInstance()
    if (!submissionsConnector.getUserAgreementState().isSolutionSharingAllowed()) return emptyMap()

    return submissionsConnector.getSharedSolutionsForCourse(course.id, course.marketplaceCourseVersion).groupBy { it.taskId }
  }

  override fun loadSharedSolutionsForTask(course: Course, task: Task): List<MarketplaceSubmission> {
    if (course !is EduCourse || !course.isMarketplace) return emptyList()

    val submissionsConnector = MarketplaceSubmissionsConnector.getInstance()
    if (!submissionsConnector.getUserAgreementState().isSolutionSharingAllowed()) return emptyList()

    return submissionsConnector.getSharedSolutionsForTask(course, task.id)
  }

  override fun loadSubmissions(tasks: List<Task>, courseId: Int): Map<Int, List<MarketplaceSubmission>> {
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

  @RequiresBackgroundThread
  override fun isSubmissionDownloadAllowed(): Boolean {
    return MarketplaceSubmissionsConnector.getInstance().getUserAgreementState().isSubmissionDownloadAllowed()
  }

  override fun getPlatformName(): String = JET_BRAINS_ACCOUNT

  override fun doAuthorize(vararg postLoginActions: Runnable) {
    MarketplaceConnector.getInstance().doAuthorize(*postLoginActions, authorizationPlace = AuthorizationPlace.SUBMISSIONS_TAB)
  }
}