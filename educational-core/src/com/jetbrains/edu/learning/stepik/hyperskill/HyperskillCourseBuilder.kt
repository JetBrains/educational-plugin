package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

open class HyperskillCourseBuilder<T>(private val baseCourseBuilder: EduCourseBuilder<T>) : EduCourseBuilder<T> by baseCourseBuilder {
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<T>? {
    val generatorBase = baseCourseBuilder.getCourseProjectGenerator(course) ?: return null
    val hyperskillCourse = course as? HyperskillCourse ?: return null
    return HyperskillCourseProjectGenerator(generatorBase, this, hyperskillCourse)
  }
}