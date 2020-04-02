package com.jetbrains.edu.javascript.learning

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.EnvironmentChecker

class JsEnvironmentChecker : EnvironmentChecker() {
  override fun checkEnvironment(project: Project): String? {
    return null // TODO implement validation
  }
}