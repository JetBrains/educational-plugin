package com.jetbrains.edu.learning.checker

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunnerRegistry
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.checker.CheckUtils.NOT_RUNNABLE_MESSAGE
import com.jetbrains.edu.learning.checker.CheckUtils.createDefaultRunConfiguration
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import java.util.*
import java.util.concurrent.CountDownLatch


open class OutputTaskChecker(task: OutputTask, project: Project) : TaskChecker<OutputTask>(task, project) {
  companion object {
    val OUTPUT_PATTERN_NAME = "output.txt"
  }

  override fun onTaskFailed(message: String) {
    super.onTaskFailed("Incorrect output")
    CheckUtils.showTestResultsToolWindow(project, message)
  }

  override fun check(): CheckResult {
    val configuration = createDefaultRunConfiguration(project) ?: return CheckResult(CheckStatus.Unchecked, NOT_RUNNABLE_MESSAGE)
    val executor = DefaultRunExecutor.getRunExecutorInstance()
    val runner = RunnerRegistry.getInstance().getRunner(executor.id, configuration.configuration)
    configuration.isActivateToolWindowBeforeRun = false
    val env = ExecutionEnvironmentBuilder.create(executor, configuration).build()
    var processNotStarted = false
    val connection = project.messageBus.connect()
    val latch = CountDownLatch(1)
    val output = ArrayList<String>()
    connection.subscribe(ExecutionManager.EXECUTION_TOPIC, object : ExecutionListener {
      override fun processNotStarted(executorId: String, e: ExecutionEnvironment) {
        if (executorId == executor.id && e == env) {
          latch.countDown()
          processNotStarted = true
        }
      }
    })
    runner?.execute(env) {
      it.processHandler?.addProcessListener(object : ProcessAdapter() {
        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
          if (outputType == ProcessOutputTypes.STDOUT) {
            output.add(event.text)
          }
        }

        override fun processTerminated(event: ProcessEvent) {
          latch.countDown()
        }
      })
    }

    latch.await()
    connection.disconnect()
    if (processNotStarted) {
      return CheckResult(CheckStatus.Unchecked, NOT_RUNNABLE_MESSAGE)
    }

    val outputPatternFile = task.getTaskDir(project)?.findChild(OUTPUT_PATTERN_NAME)
            ?: return CheckResult.FAILED_TO_CHECK
    val expectedOutput = VfsUtil.loadText(outputPatternFile)
    var outputString = output.joinToString("")
    if (outputString.isEmpty()) {
      outputString = "<no output>"
    }
    if (expectedOutput.dropLastLineBreak() == outputString.dropLastLineBreak()) {
      return CheckResult(CheckStatus.Solved, TestsOutputParser.CONGRATULATIONS)
    }
    return CheckResult(CheckStatus.Failed, "Expected output:\n$expectedOutput \nActual output:\n$outputString")
  }

  override fun clearState() {
    CheckUtils.drawAllPlaceholders(project, task)
  }

  private fun String.dropLastLineBreak() : String = if (this.endsWith('\n')) this.dropLast(1) else this
}