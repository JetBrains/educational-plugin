package com.jetbrains.edu.learning.codeforces.update

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.learning.update.CourseUpdateChecker
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class CodeforcesCourseUpdateChecker(project: Project) : CourseUpdateChecker(project) {
  private val isCourseOngoing: Boolean
    get() = (course as? CodeforcesCourse)?.isOngoing ?: false

  override val checkInterval: Long
    get() = if (isCourseOngoing) ONGOING_COURSE_CHECK_INTERVAL else super.checkInterval

  override fun courseCanBeUpdated(): Boolean = course is CodeforcesCourse

  override fun doCheckIsUpToDate(onFinish: () -> Unit) {
    val codeforcesCourse = course as? CodeforcesCourse ?: return
    CodeforcesCourseUpdater(project, codeforcesCourse).updateCourse { onFinish() }
  }

  companion object {
    val ONGOING_COURSE_CHECK_INTERVAL: Long = TimeUnit.MINUTES.toMillis(1)

    fun getInstance(project: Project): CodeforcesCourseUpdateChecker {
      return project.service()
    }
  }
}
