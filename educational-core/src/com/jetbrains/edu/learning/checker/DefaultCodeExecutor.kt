package com.jetbrains.edu.learning.checker

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.checker.CodeExecutor.Companion.resultUnchecked
import com.jetbrains.edu.learning.checker.CodeExecutor.Companion.LOG
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.runReadActionInSmartMode
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.*

open class DefaultCodeExecutor : CodeExecutor {
  override fun execute(project: Project, task: Task, indicator: ProgressIndicator, input: String?): Result<String, CheckResult> {
    val configuration = runReadActionInSmartMode(project) { createRunConfiguration(project, task) }
    if (configuration == null) {
      return resultUnchecked("Run configuration can't be created")
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
          } catch (e: IOException) {
            LOG.error("Failed to write input", e)
          }
        }
      }
    }

    val output = ArrayList<String>()
    val processListener = object : ProcessAdapter() {
      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        if (outputType == ProcessOutputTypes.STDOUT) {
          output.add(event.text)
        }
      }
    }

    CheckUtils.executeRunConfigurations(
      project,
      listOf(configuration),
      indicator,
      executionListener = executionListener,
      processListener = processListener
    )

    if (indicator.isCanceled) return resultUnchecked("Canceled")

    if (processNotStarted) {
      return resultUnchecked("Process isn't started")
    }

    var outputString = output.joinToString("")
    if (outputString.isEmpty()) {
      outputString = "<no output>"
    }
    return Ok(outputString)
  }
}
