package com.jetbrains.edu.python.learning

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.python.configuration.PyConfigurableInterpreterList

internal class PyCharmPyDirectoryProjectGenerator(course: Course) : PyDirectoryProjectGenerator(course) {

  override fun getAllSdks(): List<Sdk> {
    // New python API passes default project into `ServiceManager.getService` if project argument is null
    // but old API passes project argument into `ServiceManager.getService` directly
    // and doesn't support null argument
    // so if we use old API we should pass default project manually
    val project = if (myHasOldPythonApi) ProjectManager.getInstance().defaultProject else null
    return PyConfigurableInterpreterList.getInstance(project).allPythonSdks
  }
}
