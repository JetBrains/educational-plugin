package com.jetbrains.edu.python.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiManager
import com.intellij.util.text.nullize
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.CheckUtils.createRunConfiguration
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.tests.TestResultGroup
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.messages.EduFormatBundle
import com.jetbrains.edu.python.learning.getCurrentTaskVirtualFile
import com.jetbrains.edu.python.learning.run.PyRunTestsConfigurationProducer

/**
 * Checker for legacy python courses
 * This checker is now used only in old Hyperskill projects
 * @see com.jetbrains.edu.python.learning.PyConfigurator
 */
open class PyTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) : EduTaskCheckerBase(task, envChecker, project) {

  override fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings> {
    val producer = RunConfigurationProducer.getInstance(PyRunTestsConfigurationProducer::class.java)
    val taskDir = task.getDir(project.courseDir) ?: return emptyList()
    val testFilePath = task.course.configurator?.testFileName ?: return emptyList()
    val file = taskDir.findFileByRelativePath(testFilePath) ?: return emptyList()
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return emptyList()
    val context = ConfigurationContext(psiFile)
    val configurationFromContext = producer.findOrCreateConfigurationFromContext(context)
    return listOfNotNull(configurationFromContext?.configurationSettings)
  }

  override fun computePossibleErrorResult(indicator: ProgressIndicator, stderr: String): CheckResult {
    val error = getSyntaxError(indicator)
    if (indicator.isCanceled) return CheckResult.CANCELED
    if (error == null) return CheckResult.SOLVED
    return CheckResult(CheckStatus.Failed, CheckUtils.SYNTAX_ERROR_MESSAGE, error)
  }

  override fun areTestsFailedToRun(testResults: List<TestResultGroup>): Boolean {
    if (super.areTestsFailedToRun(testResults)) return true
    return testResults.any { group ->
      group.results.any { testResult ->
        SYNTAX_ERRORS.any { it in testResult.message }
      }
    }
  }

  private fun getSyntaxError(indicator: ProgressIndicator): String? {
    val configuration = createRunConfiguration(project, task.getCurrentTaskVirtualFile(project)) ?: return null
    configuration.isActivateToolWindowBeforeRun = false

    val errorOutput = StringBuilder()
    val processListener = object : ProcessAdapter() {
      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        if (outputType == ProcessOutputTypes.STDERR) {
          errorOutput.append(event.text)
        }
      }
    }

    if (!CheckUtils.executeRunConfigurations(project, listOf(configuration), indicator, processListener = processListener)) {
      LOG.warn("Execution was failed while trying to obtain syntax error for user message")
    }

    return errorOutput.toString().nullize()
  }

  companion object {
    /**
     * We need this hack because:
     * 1. First error is hardcoded in test_helper
     * 2. Second error could appear if educator uses third-party testing frameworks (e.g. unittest in Coursera Algorithmic Toolbox)
     * @see com.jetbrains.edu.python.slow.checker.PyCheckErrorsTest (SyntaxErrorFromUnittest)
     */
    private val SYNTAX_ERRORS = listOf("The file contains syntax errors", EduFormatBundle.message("check.no.tests"))
  }
}
