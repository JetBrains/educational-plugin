package com.jetbrains.edu.python.learning.pycharm

import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.python.learning.PyNewConfigurator
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyNewConfigurator : PyNewConfigurator() {
  private val courseBuilder: PyNewCourseBuilder = PyNewCourseBuilder()

  override fun getCourseBuilder(): EduCourseBuilder<PyNewProjectSettings> = courseBuilder
  override fun isEnabled(): Boolean = PlatformUtils.isPyCharm() || PlatformUtils.isCLion()
}
