package com.jetbrains.edu.learning.handlers

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.runInEdt
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
import java.util.concurrent.CountDownLatch

open class DefaultCodeExecutor : CodeExecutor {
  override fun execute(project: Project, task: Task, input: String?): Result<String, String> {
    val configuration = createTestConfiguration(project, task)
    if (configuration == null) {
      return Err("Run configuration can't be created")
    }
    val executor = DefaultRunExecutor.getRunExecutorInstance()
    val runner = ProgramRunner.getRunner(executor.id, configuration.configuration)
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

      override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        if (input != null) {
          val out = BufferedWriter(OutputStreamWriter(handler.processInput!!, StandardCharsets.UTF_8))
          out.write(input)
          out.write("\n")
          out.flush()
        }
      }
    })

    runInEdt {
      runner?.execute(env) { descriptor ->
        descriptor.processHandler?.addProcessListener(object : ProcessAdapter() {
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
    }

    latch.await()
    connection.disconnect()
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