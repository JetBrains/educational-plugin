package com.jetbrains.edu.javascript.learning

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestFiles
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class JsEduTaskChecker(task: EduTask, project: Project) : EduTaskCheckerBase(task, project) {

  override fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    return task.getAllTestFiles(project).mapNotNull { ConfigurationContext(it).configuration }
  }

  override val SMTestProxy.comparisonMessage: String get() {
    // It is tested only with Jest so may not work with other JS test frameworks
    val index = StringUtil.indexOfIgnoreCase(errorMessage, "Expected:", 0)
    return if (index != -1) errorMessage.substring(0, index).trim() else errorMessage
  }
}
