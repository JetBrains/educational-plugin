package com.jetbrains.edu.learning.codeforces.update

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.text.DateFormatUtil
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.update.CourseUpdateChecker

@Service
class CodeforcesCourseUpdateChecker(project: Project) : CourseUpdateChecker(project) {
  private val isCourseOngoing: Boolean
    get() = (course as? CodeforcesCourse)?.isOngoing() ?: false

  override val checkInterval: Long
    get() = if (isCourseOngoing) ONGOING_COURSE_CHECK_INTERVAL else super.checkInterval

  override fun courseCanBeUpdated(): Boolean = course is CodeforcesCourse

  override fun doCheckIsUpToDate(onFinish: () -> Unit) {
    val codeforcesCourse = course as? CodeforcesCourse ?: return
    CodeforcesCourseUpdater(project, codeforcesCourse).updateCourse { onFinish() }
  }

  companion object {
    const val ONGOING_COURSE_CHECK_INTERVAL: Long = 60 * DateFormatUtil.SECOND

    @JvmStatic
    fun getInstance(project: Project): CodeforcesCourseUpdateChecker {
      return project.service()
    }
  }
}
