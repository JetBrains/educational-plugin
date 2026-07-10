package com.jetbrains.edu.python.learning.environment

import com.intellij.openapi.project.Project
import com.intellij.python.community.services.systemPython.SystemPython
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.environment.InstallationResult
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironment
import com.jetbrains.edu.python.learning.installRequiredPackages
import com.jetbrains.python.Result
import com.jetbrains.python.packaging.PyVersionSpecifiers
import com.jetbrains.python.projectCreation.SystemPythonRequirements
import com.jetbrains.python.projectCreation.createVenvAndSdk
import com.jetbrains.python.sdk.ModuleOrProject

sealed class PyLanguageEnvironment : LanguageEnvironment {
  data class Existing(val systemPython: SystemPython, val title: String, val secondaryText: String) : PyLanguageEnvironment()
  data class Install(val versionSpecifiers: PyVersionSpecifiers) : PyLanguageEnvironment()

  override suspend fun installIfNeeded(project: Project, course: Course): InstallationResult {
    val systemPythonRequirements = when (this) {
      is Install -> SystemPythonRequirements.ByVersionSpecifier(versionSpecifiers = versionSpecifiers, confirmInstallation = { true })
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