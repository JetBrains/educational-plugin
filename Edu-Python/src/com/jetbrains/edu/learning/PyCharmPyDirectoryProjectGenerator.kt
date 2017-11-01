package com.jetbrains.edu.learning

import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.python.configuration.PyConfigurableInterpreterList

internal class PyCharmPyDirectoryProjectGenerator(course: Course) : PyDirectoryProjectGenerator(course) {

  override fun getAllSdks(): List<Sdk> = PyConfigurableInterpreterList.getInstance(null).allPythonSdks
}
