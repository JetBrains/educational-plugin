package com.jetbrains.edu.python.learning.stepik.hyperskill.pycharm

import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.python.learning.pycharm.PyConfigurator
import com.jetbrains.edu.python.learning.stepik.hyperskill.PyHyperskillConfigurator

class PyHyperskillConfigurator : PyHyperskillConfigurator() {
  private val pyConfigurator = PyConfigurator()

  override fun getCourseBuilder() = PyHyperskillCourseBuilder()
  override fun isEnabled(): Boolean = pyConfigurator.isEnabled && !EduUtils.isAndroidStudio()
}
