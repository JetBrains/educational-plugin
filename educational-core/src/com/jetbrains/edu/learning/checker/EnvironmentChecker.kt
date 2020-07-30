package com.jetbrains.edu.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task

open class EnvironmentChecker {
  open fun checkEnvironment(project: Project, task: Task): String? = null
}