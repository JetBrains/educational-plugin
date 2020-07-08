package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.CourseUpdateChecker
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse

@Service
class CodeforcesCourseUpdateChecker(project: Project) : CourseUpdateChecker(project) {
  private var isCourseOngoing: Boolean = (course as? CodeforcesCourse)?.isOngoing() ?: false

  init {
    if (isCourseOngoing) {
      setCustomCheckInterval(ONGOING_COURSE_CHECK_INTERVAL_SECONDS)
    }
  }

  override fun courseCanBeUpdated(): Boolean = course is CodeforcesCourse

  override fun doCheckIsUpToDate(onFinish: () -> Unit) {
    val codeforcesCourse = course as? CodeforcesCourse ?: return
    CodeforcesCourseUpdater(project, codeforcesCourse).updateCourseAndDoActions(
      onFinish = { onFinish() }
    )
    if (isCourseOngoing && !(course as CodeforcesCourse).isOngoing()) {
      isCourseOngoing = false
      setDefaultCheckInterval()
    }
  }

  private fun setDefaultCheckInterval() {
    LOG.info("Setting default check interval for ${course?.name}")
    checkInterval = getDefaultCheckInterval()
  }

  companion object {
    const val ONGOING_COURSE_CHECK_INTERVAL_SECONDS: Int = 60

    @JvmStatic
    fun getInstance(project: Project): CodeforcesCourseUpdateChecker {
      return project.service()
    }
  }
}
