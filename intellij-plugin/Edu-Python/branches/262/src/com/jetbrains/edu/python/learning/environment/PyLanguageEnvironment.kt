package com.jetbrains.edu.python.learning.environment

import com.intellij.openapi.project.Project
import com.intellij.python.community.services.systemPython.SystemPython
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.environment.InstallationResult
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironment
import com.jetbrains.edu.python.learning.installRequiredPackages
import com.jetbrains.python.Result
import com.jetbrains.python.projectCreation.SystemPythonRequirements
import com.jetbrains.python.projectCreation.createVenvAndSdk
import com.jetbrains.python.sdk.ModuleOrProject

sealed class PyLanguageEnvironment : LanguageEnvironment {
  data class Existing(val systemPython: SystemPython, val title: String, val secondaryText: String) : PyLanguageEnvironment()
  // TODO allow specifying the version to install, now we always install the latest version
  data object Install : PyLanguageEnvironment()

  override suspend fun installIfNeeded(project: Project, course: Course): InstallationResult {
    val systemPythonRequirements = when (this) {
      Install -> SystemPythonRequirements.ByVersionSpecifier(confirmInstallation = { true })
      is Existing -> SystemPythonRequirements.Explicit(systemPython)
    }

    val sdkResult = createVenvAndSdk(
      ModuleOrProject.ProjectOnly(project), systemPythonRequirements
    )

    return when (sdkResult) {
      is Result.Failure -> {
        InstallationResult.Error(sdkResult.error.message)
      }

      is Result.Success -> {
        installRequiredPackages(project, sdkResult.result)
        InstallationResult.Installed
      }
    }
  }
}