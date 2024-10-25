package com.jetbrains.edu.javascript.learning.checker

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.javascript.learning.NodeJS
import com.jetbrains.edu.javascript.learning.messages.EduJavaScriptBundle
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_JS
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class JsEnvironmentChecker : EnvironmentChecker() {
  override fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter
    return if (interpreter == null || interpreter.validate(project) != null) {
      return CheckResult(CheckStatus.Unchecked,
                         EduJavaScriptBundle.message("error.no.interpreter", NodeJS, ENVIRONMENT_CONFIGURATION_LINK_JS))
    }
    else null
  }
}
