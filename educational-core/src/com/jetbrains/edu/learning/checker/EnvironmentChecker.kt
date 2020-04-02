package com.jetbrains.edu.learning.checker

import com.intellij.openapi.project.Project

open class EnvironmentChecker {
  open fun checkEnvironment(project: Project): String? = null
}