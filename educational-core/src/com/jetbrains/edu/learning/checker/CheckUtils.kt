package com.jetbrains.edu.learning.checker

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.process.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.text.nullize
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getCodeTaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.messages.EduFormatBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils.navigateToFirstFailedAnswerPlaceholder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object CheckUtils {
  private val COMPILATION_ERRORS = listOf("Compilation failed", "Compilation error")

  val CONGRATULATIONS = EduFormatBundle.message("check.correct.solution")
  val COMPILATION_FAILED_MESSAGE = EduCoreBundle.message("check.error.compilation.failed")
  val NOT_RUNNABLE_MESSAGE = EduCoreBundle.message("check.error.solution.not.runnable")
  val SYNTAX_ERROR_MESSAGE = EduCoreBundle.message("check.error.syntax.error")
  val ERRORS = listOf(COMPILATION_FAILED_MESSAGE, EduFormatBundle.message("error.failed.to.launch.checking"), SYNTAX_ERROR_MESSAGE)

  private fun hasFailedAnswerPlaceholders(taskFile: TaskFile): Boolean {
    return taskFile.answerPlaceholders.isNotEmpty() && taskFile.hasFailedPlaceholders()
  }

  fun navigateToFailedPlaceholder(eduState: EduState, task: Task, taskDir: VirtualFile, project: Project) {
    val selectedTaskFile = eduState.taskFile
    var editor = eduState.editor
    var taskFileToNavigate = selectedTaskFile
    var fileToNavigate = eduState.virtualFile
    if (!hasFailedAnswerPlaceholders(selectedTaskFile)) {
      for ((_, taskFile) in task.taskFiles) {
        if (hasFailedAnswerPlaceholders(taskFile)) {
          taskFileToNavigate = taskFile
          val virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
          val fileEditor = virtualFile.getEditor(project) ?: continue
          editor = fileEditor
          fileToNavigate = virtualFile
          break
        }
      }
    }
    FileEditorManager.getInstance(project).openFile(fileToNavigate, true)
    ApplicationManager.getApplication().invokeLater {
      IdeFocusManager.getInstance(project).requestFocus(editor.contentComponent, true)
    }
    navigateToFirstFailedAnswerPlaceholder(editor, taskFileToNavigate)
  }

  fun fillWithIncorrect(message: String): String =
    message.nullize(nullizeSpaces = true) ?: EduCoreBundle.message("check.incorrect")

  fun flushWindows(task: Task, taskDir: VirtualFile) {
    for ((_, taskFile) in task.taskFiles) {
      val virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
      EduUtils.flushWindows(taskFile, virtualFile)
    }
  }

  fun createDefaultRunConfiguration(project: Project, task: Task): RunnerAndConfigurationSettings? {
    val taskFile = task.getCodeTaskFile(project) ?: return null
    return runReadAction {
      val psiFile = taskFile.getDocument(project)?.toPsiFile(project) ?: return@runReadAction null
      ConfigurationContext(psiFile).configuration
    }
  }

  fun getCustomRunConfiguration(project: Project, task: Task): RunnerAndConfigurationSettings? {
    val taskDir = task.getDir(project.courseDir) ?: error("Failed to find directory of `${task.name}` task")
    val runConfigurationDir = taskDir.findChild(EduNames.RUN_CONFIGURATION_DIR) ?: return null
    val runConfigurationFile = runConfigurationDir.children.firstOrNull { it.name.endsWith(".run.xml") } ?: return null
    val path = runConfigurationFile.path
    return RunManager.getInstance(project).allSettings.find {
      it.pathIfStoredInArbitraryFileInProject == path
    }
  }

  fun hasCompilationErrors(processOutput: ProcessOutput): Boolean {
    for (error in COMPILATION_ERRORS) {
      if (processOutput.stderr.contains(error)) return true
    }
    return false
  }

  fun postProcessOutput(output: String): String {
    return output.replace(System.getProperty("line.separator"), "\n")
  }

  fun createRunConfiguration(project: Project, taskFile: VirtualFile?): RunnerAndConfigurationSettings? {
    return runReadActionInSmartMode(project) {
      val item = PsiUtilCore.findFileSystemItem(project, taskFile)
      if (item == null) null else ConfigurationContext(item).configuration
    }
  }

  /**
   * @return true if execution finished successfully, false otherwise
   */
  fun executeRunConfigurations(
    project: Project,
    configurations: List<RunnerAndConfigurationSettings>,
    indicator: ProgressIndicator,
    executionListener: ExecutionListener? = null,
    processListener: ProcessListener? = null,
    testEventsListener: SMTRunnerEventsListener? = null
  ): Boolean {
    val connection = project.messageBus.connect()
    try {
      return executeRunConfigurations(connection, configurations, indicator, executionListener, processListener, testEventsListener)
    }
    finally {
      connection.disconnect()
    }
  }

  private fun executeRunConfigurations(
    connection: MessageBusConnection,
    configurations: List<RunnerAndConfigurationSettings>,
    indicator: ProgressIndicator,
    executionListener: ExecutionListener?,
    processListener: ProcessListener?,
    testEventsListener: SMTRunnerEventsListener?
  ): Boolean {
    if (configurations.isEmpty()) return true

    testEventsListener?.let { connection.subscribe(SMTRunnerEventsListener.TEST_STATUS, it) }

    val rootDisposable = Disposer.newDisposable()
    val latch = CountDownLatch(configurations.size)
    var isConfigurationBroken = false

    runInEdt {
      val environments = mutableListOf<ExecutionEnvironment>()
      connection.subscribe(ExecutionManager.EXECUTION_TOPIC,
                           CheckExecutionListener(DefaultRunExecutor.EXECUTOR_ID, environments, latch, executionListener))

      for (configuration in configurations) {
        if (isConfigurationBroken) {
          latch.countDown()
          continue
        }
        val runner = ProgramRunner.getRunner(DefaultRunExecutor.EXECUTOR_ID, configuration.configuration)
        val env = ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), configuration).activeTarget().build()

        if (runner == null || env.state == null) {
          latch.countDown()
          isConfigurationBroken = true
          continue
        }
        @Suppress("UnstableApiUsage")
        env.callback = ProgramRunner.Callback { descriptor ->
          // Descriptor can be null in some cases.
          // For example, IntelliJ Rust's test runner provides null here if compilation fails
          if (descriptor == null) {
            latch.countDown()
            return@Callback
          }

          Disposer.register(rootDisposable, Disposable {
            ExecutionManagerImpl.stopProcess(descriptor)
          })
          val processHandler = descriptor.processHandler
          if (processHandler != null) {
            processHandler.addProcessListener(object : ProcessAdapter() {
              override fun processTerminated(event: ProcessEvent) {
                latch.countDown()
              }
            })
            processListener?.let { processHandler.addProcessListener(it) }
          }
        }

        environments.add(env)
        runner.execute(env)
      }
    }

    while (!indicator.isCanceled) {
      val result = latch.await(100, TimeUnit.MILLISECONDS)
      if (result) break
    }
    if (indicator.isCanceled) {
      Disposer.dispose(rootDisposable)
    }
    return !isConfigurationBroken
  }

  private class CheckExecutionListener(
    private val executorId: String,
    private val environments: List<ExecutionEnvironment>,
    private val latch: CountDownLatch,
    private val delegate: ExecutionListener?
  ) : ExecutionListener {
    override fun processStartScheduled(executorId: String, env: ExecutionEnvironment) {
      checkAndExecute(executorId, env) {
        delegate?.processStartScheduled(executorId, env)
      }
    }

    override fun processNotStarted(executorId: String, env: ExecutionEnvironment) {
      checkAndExecute(executorId, env) {
        latch.countDown()
        delegate?.processNotStarted(executorId, env)
      }
    }

    override fun processStarting(executorId: String, env: ExecutionEnvironment) {
      checkAndExecute(executorId, env) {
        delegate?.processStarting(executorId, env)
      }
    }

    override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
      checkAndExecute(executorId, env) {
        delegate?.processStarted(executorId, env, handler)
      }
    }

    override fun processTerminating(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
      checkAndExecute(executorId, env) {
        delegate?.processTerminating(executorId, env, handler)
      }
    }

    override fun processTerminated(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler, exitCode: Int) {
      checkAndExecute(executorId, env) {
        delegate?.processTerminated(executorId, env, handler, exitCode)
      }
    }

    private fun checkAndExecute(executorId: String, env: ExecutionEnvironment, action: () -> Unit) {
      if (this.executorId == executorId && env in environments) {
        action()
      }
    }
  }
}
