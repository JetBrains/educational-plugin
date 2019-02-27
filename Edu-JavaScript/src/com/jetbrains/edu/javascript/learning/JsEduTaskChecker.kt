package com.jetbrains.edu.javascript.learning

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.RunnerRegistry
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.testframework.Filter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiUtilCore
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckResultDiff
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TestsOutputParser
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.util.concurrent.CountDownLatch


class JsEduTaskChecker(task: EduTask, project: Project) : TaskChecker<EduTask>(task, project) {
  override fun check(indicator: ProgressIndicator): CheckResult {
    if (task.course.isStudy) {
      runInEdt {
        ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.RUN)?.hide(null)
      }
    }
    val connection = project.messageBus.connect()
    val testResults = mutableListOf<CheckResult>()
    connection.subscribe(SMTRunnerEventsListener.TEST_STATUS, object : SMTRunnerEventsAdapter() {

      override fun onTestingFinished(testsRoot: SMTestProxy.SMRootTestProxy) {
        testResults.add(testsRoot.toCheckResult())
      }
    })

    val configurations = mutableListOf<RunnerAndConfigurationSettings>()
    ApplicationManager.getApplication().invokeAndWait {
      configurations.addAll(createTestConfigurations())
    }

    if (configurations.isEmpty()) {
      return NO_TESTS_RUN
    }

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
        // BACKCOMPAT: 2018.2
        @Suppress("DEPRECATION")
        val runner = RunnerRegistry.getInstance().getRunner(DefaultRunExecutor.EXECUTOR_ID, configuration.configuration)
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

    if (testResults.isEmpty()) {
      return NO_TESTS_RUN
    }
    val firstFailure = testResults.firstOrNull { it.status != CheckStatus.Solved }
    return firstFailure ?: testResults.first()
  }

  /**
   * Not all the languages can create run configuration from PsiDirectory context
   * So one run configuration per test file is created
   *
   * @return Run configurations created from every test file in the task
   */
  private fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    return task.getAllTests(project).mapNotNull {
      val configuration = ConfigurationContext(it).configuration?.apply {
        isActivateToolWindowBeforeRun = !task.course.isStudy
        isTemporary = true
      }
      configuration
    }
  }

  private fun Task.getAllTests(project: Project): List<PsiFile> {
    val taskDir = getDir(project) ?: error("Failed to find dir for task $name")
    val testFiles = mutableListOf<VirtualFile>()

    VfsUtilCore.processFilesRecursively(taskDir) {
      if (EduUtils.isTestsFile(project, it)) {
        val psiFile = PsiManager.getInstance(project).findFile(it)
        if (psiFile != null) {
          testFiles.add(it)
        }
      }
      true
    }
    return PsiUtilCore.toPsiFiles(PsiManager.getInstance(project), testFiles)
  }

  private fun SMTestProxy.SMRootTestProxy.toCheckResult(): CheckResult {
    if (isPassed) {
      return CheckResult(CheckStatus.Solved, TestsOutputParser.CONGRATULATIONS)
    }

    val failedChildren = collectChildren(object : Filter<SMTestProxy>() {
      override fun shouldAccept(test: SMTestProxy): Boolean {
        return test.isLeaf && !test.isPassed
      }
    })

    val firstFailedTest = failedChildren.firstOrNull() ?: error("Testing failed although no failed tests found")
    val diff = firstFailedTest.diffViewerProvider?.let { CheckResultDiff(it.diffTitle, it.left, it.right) }

    return CheckResult(CheckStatus.Failed, removeAttributes(firstFailedTest.errorMessage), diff = diff)
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

  companion object {
    private val NO_TESTS_RUN = CheckResult(CheckStatus.Unchecked, "No tests have run")
  }
}

