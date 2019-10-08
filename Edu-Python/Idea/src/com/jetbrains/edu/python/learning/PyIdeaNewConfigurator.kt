package com.jetbrains.edu.python.learning

class PyIdeaNewConfigurator : PyNewConfiguratorBase(PyIdeaNewCourseBuilder()) {
  override fun isEnabled(): Boolean = isPythonConfiguratorEnabled()
}
