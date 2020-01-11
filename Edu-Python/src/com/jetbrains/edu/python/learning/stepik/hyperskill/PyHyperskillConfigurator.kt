package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import com.jetbrains.edu.python.learning.PyConfigurator
import com.jetbrains.edu.python.learning.PyConfigurator.TASK_PY
import com.jetbrains.edu.python.learning.PyCourseBuilder
import com.jetbrains.python.newProject.PyNewProjectSettings

open class PyHyperskillConfigurator : HyperskillConfigurator<PyNewProjectSettings> {

  private val courseBuilder: PyCourseBuilder = PyHyperskillCourseBuilder()

  override fun getCourseBuilder(): EduCourseBuilder<PyNewProjectSettings> = courseBuilder
  override fun getTestFileName(): String = PyConfigurator.TESTS_PY
  override fun getTaskCheckerProvider(): TaskCheckerProvider = PyHyperskillTaskCheckerProvider()
  override fun getTestDirs(): MutableList<String> = mutableListOf("hstest")
  override fun getMockFileName(text: String) = TASK_PY
  override fun isEnabled(): Boolean = !EduUtils.isAndroidStudio()
}
