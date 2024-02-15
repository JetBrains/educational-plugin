package com.jetbrains.edu.learning.submissions

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission

/**
 * Base class for loading submissions, should be called only from SubmissionsManager.
 *
 * @see com.jetbrains.edu.learning.submissions.SubmissionsManager
 */
interface SubmissionsProvider {

  fun loadAllSubmissions(course: Course): Map<Int, List<Submission>>

  fun loadSubmissions(tasks: List<Task>, courseId: Int): Map<Int, List<Submission>>

  fun loadSharedSolutionsForCourse(course: Course): Map<Int, List<Submission>> = mapOf()

  fun loadSharedSolutionsForTask(course: Course, task: Task): List<MarketplaceSubmission> = listOf()

  fun loadSolutionFiles(submission: MarketplaceSubmission) {}

  fun areSubmissionsAvailable(course: Course): Boolean

  fun isLoggedIn(): Boolean

  /**
   * Reflects whether submissions download is allowed for legal reasons, for Marketplace we should check User Agreement state on the remote
   */
  @RequiresBackgroundThread
  fun isSubmissionDownloadAllowed(): Boolean = true

  /**
   * Reflects whether Solution Sharing is allowed for legal reasons, for Marketplace we should check User Agreement state on the remote
   */
  @RequiresBackgroundThread
  fun isSolutionSharingAllowed(): Boolean = false

  fun getPlatformName(): String

  fun doAuthorize(vararg postLoginActions: Runnable)

  companion object {
    private val EP_NAME = ExtensionPointName.create<SubmissionsProvider>("Educational.submissionsProvider")

    fun getSubmissionsProviderForCourse(course: Course): SubmissionsProvider? {
      val submissionsProviders = EP_NAME.extensionList.filter { it.areSubmissionsAvailable(course) }
      if (submissionsProviders.isEmpty()) {
        return null
      }
      if (submissionsProviders.size > 1) {
        error("Several submissionsProviders available for ${course.name}: $submissionsProviders")
      }
      return submissionsProviders[0]
    }
  }
}