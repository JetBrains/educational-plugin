package com.jetbrains.edu.python.learning.pycharm

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.python.learning.newproject.PyDirectoryProjectGenerator
import com.jetbrains.python.configuration.PyConfigurableInterpreterList

internal class PyDirectoryProjectGenerator(course: Course) : PyDirectoryProjectGenerator(course) {

  override fun getAllSdks(): List<Sdk> = PyConfigurableInterpreterList.getInstance(null).allPythonSdks

  override fun updateSdkIfNeeded(project: Project, sdk: Sdk?): Sdk? = sdk
}
