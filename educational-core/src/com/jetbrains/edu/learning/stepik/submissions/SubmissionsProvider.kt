package com.jetbrains.edu.learning.stepik.submissions

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.api.Submission

/**
 * Base class for loading submissions, should be called only from SubmissionsManager.
 *
 * @see com.jetbrains.edu.learning.stepik.SubmissionsManager
 */
interface SubmissionsProvider {

  fun loadAllSubmissions(project: Project, course: Course): Map<Int, MutableList<Submission>>

  fun loadSubmissions(stepIds: Set<Int>): Map<Int, MutableList<Submission>>

  fun loadStepSubmissions(stepId: Int): List<Submission>

  fun submissionsCanBeShown(course: Course): Boolean

  fun isLoggedIn(): Boolean

  fun getPlatformName(): String

  fun doAuthorize()

  companion object {
    private val EP_NAME = ExtensionPointName.create<SubmissionsProvider>("Educational.submissionsProvider")

    @JvmStatic
    fun getSubmissionsProviderForCourse(course: Course): SubmissionsProvider? {
      val submissionsProviders = EP_NAME.extensionList.filter { it.submissionsCanBeShown(course) }
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