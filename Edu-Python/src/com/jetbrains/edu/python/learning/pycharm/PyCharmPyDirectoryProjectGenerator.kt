package com.jetbrains.edu.python.learning.pycharm

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.python.learning.newproject.PyDirectoryProjectGenerator
import com.jetbrains.python.configuration.PyConfigurableInterpreterList

internal class PyCharmPyDirectoryProjectGenerator(course: Course) : PyDirectoryProjectGenerator(course) {
  override fun addSdk(project: Project, sdk: Sdk) {
    val model = PyConfigurableInterpreterList.getInstance(project).model
    model.addSdk(sdk)
    try {
      model.apply()
    } catch (e: ConfigurationException) {
      LOG.error("Error adding detected python interpreter " + e.message, e)
    }
  }

  override fun getAllSdks(project: Project): List<Sdk> = PyConfigurableInterpreterList.getInstance(project).allPythonSdks

  companion object {
    private val LOG: Logger = Logger.getInstance(PyCharmPyDirectoryProjectGenerator::class.java)
  }
}

