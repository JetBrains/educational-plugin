package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.python.learning.isPythonConfiguratorEnabled

class PyPyCharmHyperskillConfigurator : PyHyperskillConfiguratorBase(PyPyCharmHyperskillCourseBuilder()) {
  override fun isEnabled(): Boolean = isPythonConfiguratorEnabled() && !EduUtils.isAndroidStudio()
}
