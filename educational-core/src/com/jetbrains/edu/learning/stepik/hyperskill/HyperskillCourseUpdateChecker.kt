package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.CourseUpdateChecker
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class HyperskillCourseUpdateChecker(
  project: Project,
  course: HyperskillCourse,
  disposable: Disposable
) : CourseUpdateChecker<HyperskillCourse>(project, course, disposable) {

  override fun Course.canBeUpdated(): Boolean = course.isStudy

  override fun doCheckIsUpToDate(onFinish: () -> Unit) {
    HyperskillCourseUpdater.updateCourse(project, course) { onFinish() }
  }
}
