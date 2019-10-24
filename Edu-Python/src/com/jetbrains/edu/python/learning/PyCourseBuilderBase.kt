package com.jetbrains.edu.python.learning

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.python.newProject.PyNewProjectSettings

abstract class PyCourseBuilderBase : EduCourseBuilder<PyNewProjectSettings> {
  override val taskTemplateName: String? = PyConfiguratorBase.TASK_PY
  override val testTemplateName: String? = PyConfiguratorBase.TESTS_PY
}
