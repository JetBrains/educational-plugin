package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.update.CourseUpdateChecker
import java.util.*

@Service
class HyperskillCourseUpdateChecker(project: Project) : CourseUpdateChecker(project) {

  private val timeSinceUpdate: Long
    get() {
      val hyperskillCourse = course as HyperskillCourse
      return Date().time - hyperskillCourse.updateDate.time
    }

  override fun courseCanBeUpdated(): Boolean {
    val hyperskillCourse = course as? HyperskillCourse ?: return false
    return hyperskillCourse.isStudy
  }

  override val checkInterval: Long
    get() = if (timeSinceUpdate > super.checkInterval) {
      super.checkInterval
    }
    else {
      super.checkInterval - timeSinceUpdate
    }

  override fun doCheckIsUpToDate(onFinish: () -> Unit) {
    val hyperskillCourse = course as HyperskillCourse
    if (timeSinceUpdate < super.checkInterval) {
      onFinish()
      return
    }
    HyperskillCourseUpdater(project, hyperskillCourse).updateCourse { onFinish() }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): HyperskillCourseUpdateChecker {
      return project.service()
    }
  }
}
