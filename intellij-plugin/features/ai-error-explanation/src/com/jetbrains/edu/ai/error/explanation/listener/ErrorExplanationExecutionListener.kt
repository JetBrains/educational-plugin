package com.jetbrains.edu.ai.error.explanation.listener

import com.intellij.execution.ExecutionListener
import com.intellij.execution.OutputListener
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.error.explanation.ErrorExplanationManager
import com.jetbrains.edu.learning.checker.CheckUtils.isEduTaskEnvironment
import com.jetbrains.edu.learning.marketplace.isMarketplaceStudentCourse

/**
 * Collects stderr for error explanation after a failed run configuration.
 * Ignores run configurations created by edu plugin.
 */
class ErrorExplanationExecutionListener(private val project: Project) : ExecutionListener {
  override fun processStarting(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
    super.processStarted(executorId, env, handler)

    if (!project.isMarketplaceStudentCourse() || env.isEduTaskEnvironment) return

    val outputListener = object : OutputListener() {
      override fun processTerminated(event: ProcessEvent) {
        super.processTerminated(event)
        LOG.info("Process terminated with exit code ${event.exitCode}")

        val stderr = output.stderr.takeIf { output.exitCode != 0 && it.isNotEmpty() }
        ErrorExplanationManager.getInstance(project).setStderr(stderr)
      }
    }

    handler.addProcessListener(outputListener)
  }

  companion object {
    private val LOG = logger<ErrorExplanationExecutionListener>()
  }
}