package com.jetbrains.edu.python.learning.checkio

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.PyCourseBuilder
import com.jetbrains.edu.python.learning.newproject.PyProjectSettings

class PyCheckiOCourseBuilder : PyCourseBuilder() {
  override fun taskTemplateName(course: Course): String? = null
  override fun mainTemplateName(course: Course): String? = null
  override fun testTemplateName(course: Course): String? = null

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyProjectSettings> =
    PyCheckiOCourseProjectGenerator(this, course)
}
