package com.jetbrains.edu.python.learning.checker

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiManager
import com.intellij.util.text.nullize
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.editor.ACTION_TEXT
import com.jetbrains.edu.learning.editor.BROKEN_SOLUTION_ERROR_TEXT_END
import com.jetbrains.edu.learning.editor.BROKEN_SOLUTION_ERROR_TEXT_START
import com.jetbrains.edu.python.learning.createRunConfiguration
import com.jetbrains.edu.python.learning.getCurrentTaskVirtualFile
import com.jetbrains.edu.python.learning.run.PyCCRunTestsConfigurationProducer
import java.util.concurrent.CountDownLatch

/**
 * Checker for legacy test_helper.py
 */
open class PyTaskChecker(task: EduTask, project: Project) : EduTaskCheckerBase(task, project) {

  override fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    val producer = RunConfigurationProducer.getInstance(PyCCRunTestsConfigurationProducer::class.java)
    val taskDir = task.getTaskDir(project) ?: return emptyList()
    val testFilePath = task.course.configurator?.testFileName ?: return emptyList()
    val file = taskDir.findFileByRelativePath(testFilePath) ?: return emptyList()
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return emptyList()
    val context = ConfigurationContext(psiFile)
    val configurationFromContext = producer.findOrCreateConfigurationFromContext(context)
    return listOfNotNull(configurationFromContext?.configurationSettings)
  }

  override fun check(indicator: ProgressIndicator): CheckResult {
    if (!task.isValid(project)) {
      return CheckResult(CheckStatus.Unchecked,
                         BROKEN_SOLUTION_ERROR_TEXT_START + ACTION_TEXT + BROKEN_SOLUTION_ERROR_TEXT_END)
    }
    return super.check(indicator)
  }

  /* We can reach this method only if we have syntax error */
  override fun checkIfFailedToRunTests(stderr: String): CheckResult = CheckResult(CheckStatus.Failed, CheckUtils.SYNTAX_ERROR_MESSAGE,
                                                                                  stderr)

  override fun isSyntaxErrorHidden(result: CheckResult, stderr: StringBuilder): Boolean {
    if (result.message != "The file contains syntax errors") return false
    val error = getSyntaxError() ?: return false
    stderr.append(error)
    return true
  }

  private fun getSyntaxError(): String? {
    val connection = project.messageBus.connect()
    val configuration = createRunConfiguration(project, task.getCurrentTaskVirtualFile(project)) ?: return null
    val executor = DefaultRunExecutor.getRunExecutorInstance()
    val runner = ProgramRunner.getRunner(executor.id, configuration.configuration)
    configuration.isActivateToolWindowBeforeRun = false
    val env = ExecutionEnvironmentBuilder.create(executor, configuration).build()
    val latch = CountDownLatch(1)
    val errorOutput = StringBuilder()
    try {
      runInEdt {
        connection.subscribe(ExecutionManager.EXECUTION_TOPIC, object : ExecutionListener {
          override fun processNotStarted(executorId: String, e: ExecutionEnvironment) {
            if (executorId == executor.id && e == env) {
              latch.countDown()
            }
          }
        })
        runner?.execute(env) { descriptor ->
          descriptor.processHandler?.addProcessListener(object : ProcessAdapter() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
              if (outputType == ProcessOutputTypes.STDERR) {
                errorOutput.append(event.text)
              }
            }

            override fun processTerminated(event: ProcessEvent) {
              latch.countDown()
            }
          })
        }
      }

      latch.await()
    }
    catch (e: Exception) {
      LOG.error(e)
    }

    return errorOutput.toString().nullize()
  }

  override fun onTaskFailed(message: String, details: String?) {
    super.onTaskFailed(message, details)
    ApplicationManager.getApplication().invokeLater {
      val taskDir = task.getTaskDir(project)
      if (taskDir == null) return@invokeLater
      for ((_, taskFile) in task.taskFiles) {
        if (taskFile.answerPlaceholders.size < 2) {
          continue
        }
        val course = task.lesson.course
        if (course.isStudy) {
          runUndoTransparentWriteAction {
            PySmartChecker.runSmartTestProcess(taskDir, PyTestRunner(taskDir), taskFile, project)
          }
        }
      }
      CheckUtils.navigateToFailedPlaceholder(EduState(EduUtils.getSelectedEduEditor(project)), task, taskDir, project)
    }
  }
}
