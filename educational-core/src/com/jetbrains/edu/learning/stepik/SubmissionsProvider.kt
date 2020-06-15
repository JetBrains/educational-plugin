package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.api.Submission

/**
 * Base class for loading submissions, should be called only from SubmissionsManager.
 *
 * @see com.jetbrains.edu.learning.stepik.SubmissionsManager
 */
abstract class SubmissionsProvider {

  abstract fun loadAllSubmissions(project: Project, course: Course?): Map<Int, MutableList<Submission>>

  abstract fun loadSubmissions(stepIds: Set<Int>): Map<Int, MutableList<Submission>>

  abstract fun loadStepSubmissions(stepId: Int): List<Submission>

  abstract fun submissionsCanBeShown(course: Course?): Boolean

  abstract fun isLoggedIn(): Boolean

  abstract fun getPlatformName(): String

  abstract fun doAuthorize()

  companion object {
    private val EP_NAME = ExtensionPointName.create<SubmissionsProvider>("Educational.submissionsProvider")

    @JvmStatic
    fun getSubmissionsProviderForCourse(course: Course?): SubmissionsProvider? {
      if (course == null) return null
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