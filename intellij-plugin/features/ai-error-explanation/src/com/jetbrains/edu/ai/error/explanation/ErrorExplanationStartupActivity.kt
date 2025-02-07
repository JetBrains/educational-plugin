package com.jetbrains.edu.ai.error.explanation

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.OutputListener
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.languageById

class ErrorExplanationStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!project.isStudentProject()) return
    project.messageBus.connect().subscribe(ExecutionManager.EXECUTION_TOPIC, object : ExecutionListener {
      override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        if (env.getUserData(CheckUtils.EDU_ENV_KEY) == true) {
          super.processStarted(executorId, env, handler)
          return
        }

        handler.addProcessListener(object : OutputListener() {
          override fun startNotified(event: ProcessEvent) {
            LOG.info("Process execution started.")
          }

          override fun processTerminated(event: ProcessEvent) {
            super.processTerminated(event)
            LOG.info("Process terminated with exit code: ${event.exitCode}")
            if (output.exitCode != 0) {
              // Sometimes error messages and stack traces are in the stdout instead of stderr. For example, JS
              val outputErrorMessage = output.stderr.ifEmpty { output.stdout }
              ErrorExplanationStderrStorage.getInstance(project).setStderr(outputErrorMessage)
            }
          }
        })
        super.processStarted(executorId, env, handler)
      }
    })
  }

  companion object {
    private val LOG = Logger.getInstance(ErrorExplanationStartupActivity::class.java)
  }
}