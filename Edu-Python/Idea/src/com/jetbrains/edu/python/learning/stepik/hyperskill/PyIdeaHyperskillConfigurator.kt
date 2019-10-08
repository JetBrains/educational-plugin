package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.python.learning.isPythonConfiguratorEnabled

class PyIdeaHyperskillConfigurator : PyHyperskillConfiguratorBase(PyIdeaHyperskillCourseBuilder()) {
  override fun isEnabled(): Boolean = isPythonConfiguratorEnabled() && !EduUtils.isAndroidStudio()
}
