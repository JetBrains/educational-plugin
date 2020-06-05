package com.jetbrains.edu.javascript.learning

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestFiles
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

open class JsTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) : EduTaskCheckerBase(task, envChecker, project) {

  override fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    return task.getAllTestFiles(project).mapNotNull { ConfigurationContext(it).configuration }
  }

  // It is tested only with Jest so may not work with other JS test frameworks
  override fun getComparisonErrorMessage(node: SMTestProxy): String = extractComparisonErrorMessage(node)

  override fun getErrorMessage(node: SMTestProxy): String {
    val failedMessageStart = "Failed: \""
    return if (node.errorMessage.startsWith(failedMessageStart))
      node.errorMessage.substringAfter(failedMessageStart).substringBeforeLast('"').replace("\\\"", "\"")
    else node.errorMessage
  }
}
