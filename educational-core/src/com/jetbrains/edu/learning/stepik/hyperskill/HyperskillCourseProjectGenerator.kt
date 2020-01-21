package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

open class HyperskillCourseProjectGenerator<T>(private val base: CourseProjectGenerator<T>, builder: HyperskillCourseBuilder<T>,
                                               private val course: HyperskillCourse) : CourseProjectGenerator<T>(builder, course) {
  override fun beforeProjectGenerated(): Boolean {
    return course.courseMode == CCUtils.COURSE_MODE || HyperskillConnector.getInstance().fillHyperskillCourse(course)
  }

  // this will not work because we can't access protected methods
  override fun afterProjectGenerated(project: Project, projectSettings: T) = base.afterProjectGenerated(project, projectSettings)
}
