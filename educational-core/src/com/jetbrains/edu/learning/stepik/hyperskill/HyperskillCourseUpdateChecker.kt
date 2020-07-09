package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.update.CourseUpdateChecker
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

@Service
class HyperskillCourseUpdateChecker(project: Project) : CourseUpdateChecker(project) {

  override fun courseCanBeUpdated(): Boolean {
    val hyperskillCourse = course as? HyperskillCourse ?: return false
    return hyperskillCourse.isStudy
  }

  override fun doCheckIsUpToDate(onFinish: () -> Unit) {
    val hyperskillCourse = course as? HyperskillCourse
    if (hyperskillCourse == null) {
      return
    }
    else {
      HyperskillCourseUpdater(project, hyperskillCourse).updateCourse { onFinish() }
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): HyperskillCourseUpdateChecker {
      return project.service()
    }
  }
}
