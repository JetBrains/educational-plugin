package com.jetbrains.edu.javascript.learning

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreErrorBundle

class JsEnvironmentChecker : EnvironmentChecker() {
  override fun checkEnvironment(project: Project, task: Task): String? {
    val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter
    return if (interpreter == null || interpreter.validate(project) != null) {
      EduCoreErrorBundle.message("error.no.interpreter", NodeJS)
    } else {
      null
    }
  }
}
