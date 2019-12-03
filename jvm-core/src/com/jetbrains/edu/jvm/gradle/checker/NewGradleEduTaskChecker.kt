package com.jetbrains.edu.jvm.gradle.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestDirectories
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

open class NewGradleEduTaskChecker(task: EduTask, project: Project) : EduTaskCheckerBase(task, project) {

  override fun computePossibleErrorResult(indicator: ProgressIndicator, stderr: String): CheckResult {
    return GradleCommandLine.create(project, "${getGradleProjectName(task)}:testClasses")?.launchAndCheck(indicator)
           ?: CheckResult.FAILED_TO_CHECK
  }

  override fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    return withGradleTestRunner(project, task) {
      task.getAllTestDirectories(project)
        .mapNotNull { ConfigurationContext(it).configuration }
    }.orEmpty()
  }

  override fun getComparisonErrorMessage(node: SMTestProxy): String {
    val index = StringUtil.indexOfIgnoreCase(node.errorMessage, "expected:", 0)
    return if (index != -1) node.errorMessage.substring(0, index).trim() else node.errorMessage
  }
}
