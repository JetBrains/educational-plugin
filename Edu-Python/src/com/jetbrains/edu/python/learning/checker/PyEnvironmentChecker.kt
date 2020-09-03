package com.jetbrains.edu.python.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.python.sdk.pythonSdk

class PyEnvironmentChecker : EnvironmentChecker() {
  override fun checkEnvironment(project: Project, task: Task): CheckResult? {
    return if (project.pythonSdk == null) {
      CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("error.no.interpreter", EduNames.PYTHON))
    }
    else null
  }
}