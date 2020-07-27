package com.jetbrains.edu.python.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.messages.EduCoreErrorBundle
import com.jetbrains.python.sdk.pythonSdk

class PyEnvironmentChecker : EnvironmentChecker() {
  override fun checkEnvironment(project: Project): String? {
    return if (project.pythonSdk == null) EduCoreErrorBundle.message("error.no.interpreter", EduNames.PYTHON) else null
  }
}