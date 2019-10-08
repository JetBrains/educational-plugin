package com.jetbrains.edu.python.learning

class PyPyCharmNewConfigurator : PyNewConfiguratorBase(PyPyCharmNewCourseBuilder()) {
  override fun isEnabled(): Boolean = isPythonConfiguratorEnabled()
}
