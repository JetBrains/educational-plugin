package com.jetbrains.edu.learning.checker

import com.intellij.execution.ExecutionListener
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.checker.CodeExecutor.Companion.resultUnchecked
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.runReadActionInSmartMode
import org.jetbrains.annotations.NonNls
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

open class DefaultCodeExecutor : CodeExecutor {
  override fun execute(project: Project, task: Task, indicator: ProgressIndicator, input: String?): Result<String, CheckResult> {
    fun logAndQuit(error: String): Err<CheckResult> {
      LOG.warn(error)
      return resultUnchecked(error)
    }

    val configuration = runReadActionInSmartMode(project) { createRunConfiguration(project, task) }
    if (configuration == null) {
      LOG.warn("Failed to launch checking. Run configuration is null")
      return Err(CheckResult.failedToCheck)
    }

    try {
      configuration.checkSettings()
    }
    catch (e: RuntimeConfigurationException) {
      LOG.warn("Failed to launch checking", e)
      return Err(CheckResult.failedToCheck)
    }

    configuration.isActivateToolWindowBeforeRun = false

    var processNotStarted = false
    val executionListener = object : ExecutionListener {
      override fun processNotStarted(executorId: String, e: ExecutionEnvironment) {
        processNotStarted = true
      }

      override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        if (input != null) {
          try {
            val out = BufferedWriter(OutputStreamWriter(handler.processInput!!, StandardCharsets.UTF_8))
            out.write(input)
            out.write("\n")
            out.flush()
          }
          catch (e: IOException) {
            LOG.warn("Failed to write input", e)
          }
        }
      }
    }

    val processListener = StdoutProcessListener()

    if (!CheckUtils.executeRunConfigurations(project, listOf(configuration), indicator, executionListener, processListener))
      return logAndQuit(EduCoreBundle.message("error.execution.failed"))

    if (indicator.isCanceled) return logAndQuit(EduCoreBundle.message("error.execution.canceled"))
    if (processNotStarted) return logAndQuit(EduCoreBundle.message("error.execution.failed"))

    var outputString = processListener.output.joinToString("")
    if (outputString.isEmpty()) {
      outputString = NO_OUTPUT
    }
    return Ok(outputString)
  }

  companion object {
    private val LOG = Logger.getInstance(DefaultCodeExecutor::class.java)

    @NonNls
    const val NO_OUTPUT: String = "<no output>"
  }
}
