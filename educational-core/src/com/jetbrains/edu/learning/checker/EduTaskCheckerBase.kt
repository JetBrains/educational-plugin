package com.jetbrains.edu.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.process.*
import com.intellij.execution.testframework.Filter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.edu.learning.checker.CheckResult.Companion.NO_TESTS_RUN
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.runReadActionInSmartMode

abstract class EduTaskCheckerBase(task: EduTask, project: Project) : TaskChecker<EduTask>(task, project) {
  var activateRunToolWindow: Boolean = !task.course.isStudy

  override fun check(indicator: ProgressIndicator): CheckResult {
    if (task.course.isStudy) {
      runInEdt {
        ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.RUN)?.hide(null)
      }
    }

    val configurations = runReadActionInSmartMode(project) { createTestConfigurations() }
    configurations.forEach {
      it.isActivateToolWindowBeforeRun = activateRunToolWindow
      it.isTemporary = true
    }

    if (configurations.isEmpty()) return NO_TESTS_RUN

    val testRoots = mutableListOf<SMTestProxy.SMRootTestProxy>()
    val testEventsListener = object : SMTRunnerEventsAdapter() {
      // We have to collect test roots in `onTestingStarted`
      // because some test framework integrations (Gradle)
      // have custom components in implementation that don't provide test events
      // except `onTestingStarted`
      override fun onTestingStarted(testsRoot: SMTestProxy.SMRootTestProxy) {
        testRoots += testsRoot
      }
    }

    val stderr = StringBuilder()
    val processListener = object : ProcessAdapter() {
      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val text = event.text
        if (text != null && ProcessOutputType.isStderr(outputType)) {
          stderr.append(text)
        }
      }
    }

    CheckUtils.executeRunConfigurations(
      project,
      configurations,
      processListener = processListener,
      testEventsListener = testEventsListener
    )

    // We need to invoke all current pending EDT actions to get proper states of test roots.
    invokeAndWaitIfNeeded {}

    if (areTestsFailedToRun(testRoots)) {
      val result = computePossibleErrorResult(stderr.toString())
      if (!result.isSolved) {
        return result
      }
    }

    val testResults = testRoots.map { it.toCheckResult() }
    if (testResults.isEmpty()) return NO_TESTS_RUN

    val firstFailure = testResults.firstOrNull { it.status != CheckStatus.Solved }
    return firstFailure ?: testResults.first()
  }

  protected fun SMTestProxy.SMRootTestProxy.toCheckResult(): CheckResult {
    if (isPassed) return CheckResult(CheckStatus.Solved, CheckUtils.CONGRATULATIONS)

    val failedChildren = collectChildren(object : Filter<SMTestProxy>() {
      override fun shouldAccept(test: SMTestProxy): Boolean = test.isLeaf && !test.isPassed
    })

    val firstFailedTest = failedChildren.firstOrNull() ?: error("Testing failed although no failed tests found")
    val diff = firstFailedTest.diffViewerProvider?.let {
      CheckResultDiff(it.left, it.right, it.diffTitle, removeAttributes(getComparisonErrorMessage(firstFailedTest)))
    }
    return CheckResult(CheckStatus.Failed,
                       message = removeAttributes(getErrorMessage(firstFailedTest)),
                       diff = diff)
  }

  /**
   * Some testing frameworks add attributes to be shown in console (ex. Jest - ANSI color codes)
   * which are not supported in Task Description, so they need to be removed
   */
  private fun removeAttributes(text: String): String {
    val buffer = StringBuilder()
    AnsiEscapeDecoder().escapeText(text, ProcessOutputTypes.STDOUT) { chunk, _ ->
      buffer.append(chunk)
    }
    return buffer.toString()
  }

  /**
   * Launches additional task if tests cannot be run (e.g. because of compilation error or syntax error).
   * Main purpose is to get proper error message for such cases.
   * If test framework support already provides correct message, you don't need to override this method
   */
  protected open fun computePossibleErrorResult(stderr: String): CheckResult = CheckResult.SOLVED

  /**
   * Check if the launch of tests was not successful. It allows us to create meaningful output in such cases.
   */
  protected open fun areTestsFailedToRun(testRoots: List<SMTestProxy.SMRootTestProxy>): Boolean = testRoots.all { it.children.isEmpty() }

  /**
   * Creates and return list of run configurations to run task tests.
   *
   * In case, when support of particular language can create single run configuration from [com.intellij.psi.PsiDirectory] context,
   * it should return only one configuration per test directory. Otherwise, returns one run configuration per test file
   *
   * @return Run configurations to run task tests
   */
  protected abstract fun createTestConfigurations(): List<RunnerAndConfigurationSettings>

  /**
   * Returns message for test error that will be shown to a user in Check Result panel
   */
  protected open fun getErrorMessage(node: SMTestProxy): String = node.errorMessage

  /**
   * Returns message for comparison error that will be shown to a user in Check Result panel
   */
  protected open fun getComparisonErrorMessage(node: SMTestProxy): String = getErrorMessage(node)
}
