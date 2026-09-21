package com.jetbrains.edu.python.learning.checker

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_PYTHON
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.python.learning.messages.EduPythonBundle
import com.jetbrains.python.sdk.pythonSdk

class PyEnvironmentChecker : EnvironmentChecker() {
  override fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    return if (detectSdk(project, task) == null) {
      CheckResult(CheckStatus.Unchecked, EduPythonBundle.message("error.no.python.interpreter", ENVIRONMENT_CONFIGURATION_LINK_PYTHON))
    }
    else {
      null
    }
  }

  private fun detectSdk(project: Project, task: Task): Sdk? {
    return project.pythonSdk ?: project.moduleSdk(task) 
  }

  private fun Project.moduleSdk(task: Task): Sdk? {
    val taskModule = task.getDir(courseDir)?.let { ModuleUtilCore.findModuleForFile(it, this) }
    return taskModule?.pythonSdk
  }
}