package com.jetbrains.edu.python.learning.checkio

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.python.learning.PyCourseBuilderBase
import com.jetbrains.edu.python.learning.PyPyCharmCourseProjectGenerator

class PyPyCharmCheckiOCourseProjectGenerator(builder: PyCourseBuilderBase, course: Course) : PyPyCharmCourseProjectGenerator(builder, course) {
  override fun beforeProjectGenerated(): Boolean = true
}
