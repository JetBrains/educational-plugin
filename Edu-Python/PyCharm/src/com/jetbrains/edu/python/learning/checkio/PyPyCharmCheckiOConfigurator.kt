package com.jetbrains.edu.python.learning.checkio

import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.python.learning.isPythonConfiguratorEnabled

class PyPyCharmCheckiOConfigurator : PyCheckiOConfiguratorBase(PyPyCharmCheckiOCourseBuilder()) {
  override fun isEnabled(): Boolean = isPythonConfiguratorEnabled() && !EduUtils.isAndroidStudio()
}
