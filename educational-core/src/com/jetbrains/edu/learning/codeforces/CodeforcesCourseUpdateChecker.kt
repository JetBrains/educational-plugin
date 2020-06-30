package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.CourseUpdateChecker
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.Course

class CodeforcesCourseUpdateChecker(project: Project,
                                    course: CodeforcesCourse,
                                    disposable: Disposable
) : CourseUpdateChecker<CodeforcesCourse>(project, course, disposable) {
  private var isCourseOngoing: Boolean = course.isOngoing()

  init {
    if (isCourseOngoing) {
      setCustomCheckInterval(ONGOING_COURSE_CHECK_INTERVAL_SECONDS)
    }
  }

  override fun Course.canBeUpdated(): Boolean = course is CodeforcesCourse

  override fun doCheckIsUpToDate(onFinish: () -> Unit) {
    CodeforcesCourseUpdater(project, course).updateCourseAndDoActions(
      onFinish = { onFinish() }
    )
    if (isCourseOngoing && !course.isOngoing()) {
      isCourseOngoing = false
      setDefaultCheckInterval()
    }
  }

  private fun setDefaultCheckInterval() {
    LOG.info("Setting default check interval for ${course.name}")
    checkInterval = getDefaultCheckInterval()
  }

  companion object {
    const val ONGOING_COURSE_CHECK_INTERVAL_SECONDS: Int = 60
  }
}
