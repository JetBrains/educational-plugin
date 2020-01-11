package com.jetbrains.edu.python.learning.checkio

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.PyCourseBuilder
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyCheckiOCourseBuilder : PyCourseBuilder() {
  override val taskTemplateName: String? = null
  override val testTemplateName: String? = null

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyNewProjectSettings>? =
    PyCheckiOCourseProjectGenerator(this, course)
}
