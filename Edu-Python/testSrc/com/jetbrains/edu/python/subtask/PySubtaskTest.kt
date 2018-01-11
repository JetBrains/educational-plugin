package com.jetbrains.edu.python.subtask

import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.subtask.CCSubtaskTestBase
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.python.learning.PyConfigurator
import com.jetbrains.edu.python.learning.PyCourseBuilder
import com.jetbrains.python.PythonLanguage

class PySubtaskTest : CCSubtaskTestBase() {
  override val courseBuilder: EduCourseBuilder<*> = PyCourseBuilder()
  override val taskFileName: String = PyConfigurator.TASK_PY
  override val testFileName: String = PyConfigurator.TESTS_PY
  override val language: Language = PythonLanguage.INSTANCE
}
