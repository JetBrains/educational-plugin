package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.PyCourseBuilder
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyHyperskillCourseBuilder : PyCourseBuilder() {
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyNewProjectSettings> =
    PyHyperskillCourseProjectGenerator(this, course)
}
