package com.jetbrains.edu.python.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiManager
import com.intellij.util.text.nullize
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.CheckUtils.createRunConfiguration
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.messages.EduFormatBundle
import com.jetbrains.edu.python.learning.getCurrentTaskVirtualFile
import com.jetbrains.edu.python.learning.messages.EduPythonBundle
import com.jetbrains.edu.python.learning.run.PyCCRunTestsConfigurationProducer
import java.io.IOException

/**
 * Checker for legacy python courses
 * @see com.jetbrains.edu.python.learning.PyConfigurator
 * @see fileTemplates.internal (test_helper.py)
 */
open class PyTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) : EduTaskCheckerBase(task, envChecker, project) {

  override fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings> {
    val producer = RunConfigurationProducer.getInstance(PyCCRunTestsConfigurationProducer::class.java)
    val taskDir = task.getDir(project.courseDir) ?: return emptyList()
    val testFilePath = task.course.configurator?.testFileName ?: return emptyList()
    val file = taskDir.findFileByRelativePath(testFilePath) ?: return emptyList()
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return emptyList()
    val context = ConfigurationContext(psiFile)
    val configurationFromContext = producer.findOrCreateConfigurationFromContext(context)
    return listOfNotNull(configurationFromContext?.configurationSettings)
  }

  override fun check(indicator: ProgressIndicator): CheckResult {
    if (!task.isValid(project)) {
      return CheckResult(CheckStatus.Unchecked, EduPythonBundle.message("error.solution.not.loaded"))
    }
    return super.check(indicator)
  }

  private fun Task.isValid(project: Project): Boolean {
    val taskDir = getDir(project.courseDir) ?: return false
    for (taskFile in taskFiles.values) {
      val file = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
      try {
        val text = VfsUtilCore.loadText(file)
        if (!taskFile.isValid(text)) return false
      }
      catch (e: IOException) {
        return false
      }
    }
    return true
  }

  override fun computePossibleErrorResult(indicator: ProgressIndicator, stderr: String): CheckResult {
    val error = getSyntaxError(indicator)
    if (indicator.isCanceled) return CheckResult.CANCELED
    if (error == null) return CheckResult.SOLVED
    return CheckResult(CheckStatus.Failed, CheckUtils.SYNTAX_ERROR_MESSAGE, error)
  }

  override fun areTestsFailedToRun(testRoots: List<SMTestProxy.SMRootTestProxy>): Boolean {
    if (super.areTestsFailedToRun(testRoots)) return true
    val result = testRoots.firstOrNull()?.toCheckResult() ?: return false
    return SYNTAX_ERRORS.any { it in result.message }
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

  override fun onTaskFailed() {
    super.onTaskFailed()
    ApplicationManager.getApplication().invokeLater {
      val taskDir = task.getDir(project.courseDir)
      if (taskDir == null) return@invokeLater
      val eduState = project.eduState ?: return@invokeLater
      CheckUtils.navigateToFailedPlaceholder(eduState, task, taskDir, project)
    }
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
