package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import com.jetbrains.edu.python.learning.PyConfiguratorBase
import com.jetbrains.edu.python.learning.PyConfiguratorBase.TASK_PY
import com.jetbrains.edu.python.learning.PyCourseBuilderBase
import com.jetbrains.python.newProject.PyNewProjectSettings

abstract class PyHyperskillConfiguratorBase(
  private val courseBuilder: PyCourseBuilderBase
) : HyperskillConfigurator<PyNewProjectSettings> {
  override fun getCourseBuilder(): EduCourseBuilder<PyNewProjectSettings> = courseBuilder
  override fun getTestFileName(): String = PyConfiguratorBase.TESTS_PY
  override fun getTaskCheckerProvider(): TaskCheckerProvider = PyHyperskillTaskCheckerProvider()
  override fun getTestDirs(): MutableList<String> = mutableListOf("hstest")
  override fun getMockFileName(text: String) = TASK_PY
}
