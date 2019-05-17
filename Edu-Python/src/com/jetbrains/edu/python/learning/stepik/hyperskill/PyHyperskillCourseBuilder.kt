package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.python.learning.PyCourseBuilder
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator

open class PyHyperskillCourseBuilder : PyCourseBuilder() {

  override fun getCourseProjectGenerator(course: Course): PyCourseProjectGenerator {
    return PyHyperskillCourseProjectGenerator(this, course)
  }
}
