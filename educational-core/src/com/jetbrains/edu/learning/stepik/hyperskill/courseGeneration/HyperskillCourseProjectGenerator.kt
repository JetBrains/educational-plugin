package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

open class HyperskillCourseProjectGenerator<T : Any>(
  private val base: CourseProjectGenerator<T>,
  builder: HyperskillCourseBuilder<T>,
  course: HyperskillCourse
) : CourseProjectGenerator<T>(builder, course) {

  override fun afterProjectGenerated(project: Project, projectSettings: T) = base.afterProjectGenerated(project, projectSettings)

  override fun createAdditionalFiles(holder: CourseInfoHolder<Course>, isNewCourse: Boolean) =
    base.createAdditionalFiles(holder, isNewCourse)

  override fun createCourseStructure(holder: CourseInfoHolder<Course>) =
    base.createCourseStructure(holder)
}
