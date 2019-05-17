package com.jetbrains.edu.learning.checker

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.Filter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.application.invokeAndWaitIfNeed
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import java.util.concurrent.CountDownLatch

abstract class EduTaskCheckerBase(task: EduTask, project: Project) : TaskChecker<EduTask>(task, project) {
  var activateRunToolWindow: Boolean = !task.course.isStudy

  override fun check(indicator: ProgressIndicator): CheckResult {
    if (task.course.isStudy) {
      runInEdt {
        ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.RUN)?.hide(null)
      }
    }
    val connection = project.messageBus.connect()
    val testRoots = mutableListOf<SMTestProxy.SMRootTestProxy>()

    connection.subscribe(SMTRunnerEventsListener.TEST_STATUS, object : SMTRunnerEventsAdapter() {
      // We have to collect test roots in `onTestingStarted`
      // because some test framework integrations (Gradle)
      // have custom components in implementation that don't provide test events
      // except `onTestingStarted`
      override fun onTestingStarted(testsRoot: SMTestProxy.SMRootTestProxy) {
        testRoots += testsRoot
      }
    })

    val configurations = DumbService.getInstance(project).runReadActionInSmartMode(Computable { createTestConfigurations() })

    if (configurations.isEmpty()) return NO_TESTS_RUN

    val latch = CountDownLatch(configurations.size)
    runInEdt {
      val environments = mutableListOf<ExecutionEnvironment>()
      connection.subscribe(ExecutionManager.EXECUTION_TOPIC, object : ExecutionListener {
        override fun processNotStarted(executorId: String, e: ExecutionEnvironment) {
          if (executorId == DefaultRunExecutor.EXECUTOR_ID && environments.contains(e)) {
            latch.countDown()
          }
        }
      })
      for (configuration in configurations) {
        val runner = ProgramRunner.getRunner(DefaultRunExecutor.EXECUTOR_ID, configuration.configuration)
        val env = ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), configuration).build()
        environments.add(env)
        runner?.execute(env) { descriptor ->
          descriptor.processHandler?.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
              latch.countDown()
            }
          })
        }
      }
    }

    latch.await()
    connection.disconnect()

    // We need to invoke all current pending EDT actions to get proper states of test roots.
    // BACKCOMPAT: 2018.3
    @Suppress("DEPRECATION")
    invokeAndWaitIfNeed {}

    if (testRoots.all { it.children.isEmpty() }) {
      val compilationResult = checkIfFailedToRunTests()
      if (!compilationResult.isSolved) {
        return compilationResult
      }
    }

    val testResults = testRoots.map { it.toCheckResult() }
    if (testResults.isEmpty()) return NO_TESTS_RUN

    val firstFailure = testResults.firstOrNull { it.status != CheckStatus.Solved }
    return firstFailure ?: testResults.first()
  }

  private fun SMTestProxy.SMRootTestProxy.toCheckResult(): CheckResult {
    if (isPassed) return CheckResult(CheckStatus.Solved, TestsOutputParser.CONGRATULATIONS)

    val failedChildren = collectChildren(object : Filter<SMTestProxy>() {
      override fun shouldAccept(test: SMTestProxy): Boolean = test.isLeaf && !test.isPassed
    })

    val firstFailedTest = failedChildren.firstOrNull() ?: error("Testing failed although no failed tests found")
    val diff = firstFailedTest.diffViewerProvider?.let {
      CheckResultDiff(it.left, it.right, it.diffTitle, removeAttributes(firstFailedTest.comparisonMessage))
    }
    return CheckResult(CheckStatus.Failed,
                       message = removeAttributes(firstFailedTest.errorMessage),
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
   * Launches additional task if tests cannot be run (e.g. because of compilation error).
   * Main purpose is to get proper error message for such cases.
   * If test framework support already provides correct message, you don't need to override this method
   */
  protected open fun checkIfFailedToRunTests(): CheckResult = CheckResult.SOLVED

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
   * Returns message that will be shown to a user in Check Result panel
   */
  protected open val SMTestProxy.comparisonMessage: String get() = errorMessage

  companion object {
    private val NO_TESTS_RUN = CheckResult(CheckStatus.Unchecked, "No tests have run")
  }
}
