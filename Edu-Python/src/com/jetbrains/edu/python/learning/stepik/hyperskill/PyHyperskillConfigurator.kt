package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import com.jetbrains.edu.python.learning.PyConfigurator
import com.jetbrains.python.newProject.PyNewProjectSettings

open class PyHyperskillConfigurator : HyperskillConfigurator<PyNewProjectSettings> {
  override fun getTestFileName(): String = PyConfigurator.TESTS_PY
  override fun getCourseBuilder() = PyHyperskillCourseBuilder()
  override fun getTaskCheckerProvider() = PyHyperskillTaskCheckerProvider()
  override fun isEnabled(): Boolean = !PlatformUtils.isPyCharm() && !PlatformUtils.isCLion() && !EduUtils.isAndroidStudio()
  override fun getTestDirs(): MutableList<String> = mutableListOf("hstest")
}
