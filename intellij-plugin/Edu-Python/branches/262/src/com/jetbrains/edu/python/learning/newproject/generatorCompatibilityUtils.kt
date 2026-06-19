package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.edu.python.learning.installRequiredPackages
import com.jetbrains.edu.python.learning.messages.EduPythonBundle
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator.Companion.LOG
import com.jetbrains.python.Result
import com.jetbrains.python.projectCreation.SystemPythonRequirements
import com.jetbrains.python.projectCreation.createVenvAndSdk
import com.jetbrains.python.sdk.ModuleOrProject

fun afterPyProjectGeneration(
  project: Project,
  projectSettings: PyProjectSettings,
  callback: () -> Unit
) {
  val systemPythonRequirements = projectSettings.sdk?.let {
    SystemPythonRequirements.Explicit(it)
  } ?: SystemPythonRequirements.ByVersionSpecifier(confirmInstallation = { true })

  val systemPython = projectSettings.sdk
  if (systemPython == null) {
    LOG.warn("No python interpreter is selected for the project")
    return
  }

  val sdkResult = runWithModalProgressBlocking(project, EduPythonBundle.message("creating.virtual.environment")) {
    createVenvAndSdk(
      ModuleOrProject.ProjectOnly(project),
      systemPythonRequirements
    )
  }

  when (sdkResult) {
    is Result.Failure -> {
      LOG.warn(sdkResult.error.message)
      return
    }

    is Result.Success -> {
      installRequiredPackages(project, sdkResult.result)
      callback()
    }
  }
}