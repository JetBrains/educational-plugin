package com.jetbrains.edu.python.learning.checkio

import com.jetbrains.edu.python.learning.PyCourseBuilder

abstract class PyCheckiOCourseBuilderBase : PyCourseBuilder() {
  override val taskTemplateName: String? = null
  override val testTemplateName: String? = null
}
