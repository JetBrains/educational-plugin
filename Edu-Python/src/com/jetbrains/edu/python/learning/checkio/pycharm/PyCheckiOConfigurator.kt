package com.jetbrains.edu.python.learning.checkio.pycharm

import com.jetbrains.edu.python.learning.checkio.PyCheckiOConfigurator
import com.jetbrains.edu.python.learning.pycharm.PyConfigurator

class PyCheckiOConfigurator : PyCheckiOConfigurator() {
  private val myPyConfigurator = PyConfigurator()
  private val myCourseBuilder = PyCheckiOCourseBuilder()

  override fun getCourseBuilder() = myCourseBuilder

  override fun isEnabled() = myPyConfigurator.isEnabled
}
