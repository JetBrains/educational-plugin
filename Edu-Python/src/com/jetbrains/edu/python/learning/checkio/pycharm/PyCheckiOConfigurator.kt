package com.jetbrains.edu.python.learning.checkio.pycharm

import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.python.learning.checkio.PyCheckiOConfigurator
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyCheckiOConfigurator : PyCheckiOConfigurator() {
  private val myCourseBuilder: PyCheckiOCourseBuilder = PyCheckiOCourseBuilder()

  override fun getCourseBuilder(): EduCourseBuilder<PyNewProjectSettings> = myCourseBuilder

  override fun isEnabled(): Boolean = PlatformUtils.isPyCharm() || PlatformUtils.isCLion()
}
