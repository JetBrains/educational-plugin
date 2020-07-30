package com.jetbrains.edu.javascript.learning

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreErrorBundle

class JsEnvironmentChecker : EnvironmentChecker() {
  override fun checkEnvironment(project: Project, task: Task): String? {
    return if (NodeJsInterpreterManager.getInstance(project).interpreter == null)
      EduCoreErrorBundle.message("no.interpreter", "Node.js")
    else null
  }
}