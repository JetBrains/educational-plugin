package com.jetbrains.edu.python.learning.pycharm

import com.intellij.openapi.application.Experiments
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.python.learning.PyNewConfigurator
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyNewConfigurator : PyNewConfigurator() {
  private val courseBuilder: PyNewCourseBuilder = PyNewCourseBuilder()

  override fun getCourseBuilder(): EduCourseBuilder<PyNewProjectSettings> = courseBuilder
  override fun isEnabled(): Boolean = (Experiments.isFeatureEnabled(EduExperimentalFeatures.PYTHON_UNITTEST) || isUnitTestMode) &&
                                      PlatformUtils.isPyCharm() || PlatformUtils.isCLion()
}
