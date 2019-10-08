package com.jetbrains.edu.python.learning.checkio

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.python.learning.PyCourseBuilderBase
import com.jetbrains.edu.python.learning.PyIdeaCourseProjectGenerator

class PyIdeaCheckiOCourseProjectGenerator(builder: PyCourseBuilderBase, course: Course) : PyIdeaCourseProjectGenerator(builder, course) {
  override fun beforeProjectGenerated(): Boolean = true
}
