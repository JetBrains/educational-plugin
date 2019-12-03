package com.jetbrains.edu.learning.checker

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.*

open class DefaultCodeExecutor : CodeExecutor {
  override fun execute(project: Project, task: Task, input: String?): Result<String, String> {
    val configuration = createTestConfiguration(project, task)
    if (configuration == null) {
      return Err("Run configuration can't be created")
    }
    configuration.isActivateToolWindowBeforeRun = false

    var processNotStarted = false
    val executionListener = object : ExecutionListener {
      override fun processNotStarted(executorId: String, e: ExecutionEnvironment) {
          processNotStarted = true
      }

      override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        if (input != null) {
          val out = BufferedWriter(OutputStreamWriter(handler.processInput!!, StandardCharsets.UTF_8))
          out.write(input)
          out.write("\n")
          out.flush()
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
      executionListener = executionListener,
      processListener = processListener
    )

    if (processNotStarted) {
      return Err("Process isn't started")
    }

    var outputString = output.joinToString("")
    if (outputString.isEmpty()) {
      outputString = "<no output>"
    }
    return Ok(outputString)
  }
}
