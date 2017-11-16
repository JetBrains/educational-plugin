package com.jetbrains.edu.learning.checker

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.RunnerRegistry
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.ide.DataManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.StudyUtils
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.StudyStatus
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import java.util.*
import java.util.concurrent.CountDownLatch


class OutputTaskChecker(task: OutputTask, project: Project) : TaskChecker<OutputTask>(task, project) {
  companion object {
    private val NOT_RUNNABLE_MESSAGE = "Solution isn't runnable"
    private val OUTPUT_PATTERN_NAME = "output.txt"
  }

  override fun onTaskFailed(message: String) {
    super.onTaskFailed("Incorrect output")
    CheckUtils.showTestResultsToolWindow(myProject, message)
  }

  override fun onTaskSolved(message: String) {
    super.onTaskSolved(message)
  }

  override fun check(): CheckResult {
    val configuration = getConfiguration() ?: return CheckResult(StudyStatus.Unchecked, NOT_RUNNABLE_MESSAGE)
    val executor = DefaultRunExecutor.getRunExecutorInstance()
    val runner = RunnerRegistry.getInstance().getRunner(executor.id, configuration.configuration)
    configuration.isActivateToolWindowBeforeRun = false
    val env = ExecutionEnvironmentBuilder.create(executor, configuration).build()
    var processNotStarted = false
    val connection = myProject.messageBus.connect()
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
      return CheckResult(StudyStatus.Unchecked, NOT_RUNNABLE_MESSAGE)
    }

    val outputPatternFile = myTask.getTaskDir(myProject)?.findChild(OUTPUT_PATTERN_NAME)
                            ?: return CheckResult(StudyStatus.Unchecked, CheckAction.FAILED_CHECK_LAUNCH)
    val expectedOutput = VfsUtil.loadText(outputPatternFile)
    var outputString = output.joinToString("")
    if (outputString.isEmpty()) {
      outputString = "<no output>"
    }
    if (expectedOutput.dropLastLineBreak() == outputString.dropLastLineBreak()) {
      return CheckResult(StudyStatus.Solved, TestsOutputParser.CONGRATULATIONS)
    }
    return CheckResult(StudyStatus.Failed, "Expected output:\n$expectedOutput \nActual output:\n$outputString")
  }

  private fun getConfiguration(): RunnerAndConfigurationSettings? {
    return ApplicationManager.getApplication().runReadAction(Computable<RunnerAndConfigurationSettings> {
      val dataContext = DataManager.getInstance().getDataContext(StudyUtils.getSelectedEditor(myProject)?.component)
      val configurationContext = ConfigurationContext.getFromContext(dataContext)
      return@Computable configurationContext.configuration
    })
  }

  override fun clearState() {
    CheckUtils.drawAllPlaceholders(myProject, myTask)
  }

  private fun String.dropLastLineBreak() : String = if (this.endsWith('\n')) this.dropLast(1) else this
}