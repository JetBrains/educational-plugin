package com.jetbrains.edu.learning.checker

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.editor.EduSingleFileEditor
import com.jetbrains.edu.learning.navigation.NavigationUtils.navigateToFirstFailedAnswerPlaceholder
import com.jetbrains.edu.learning.runReadActionInSmartMode
import java.util.concurrent.CountDownLatch

object CheckUtils {
  const val STUDY_PREFIX = "#educational_plugin"
  const val CONGRATULATIONS = "Congratulations!"
  const val TEST_OK = "test OK"
  const val TEST_FAILED = "FAILED + "
  const val CONGRATS_MESSAGE = "CONGRATS_MESSAGE "
  val COMPILATION_ERRORS = listOf("Compilation failed", "Compilation error")
  const val COMPILATION_FAILED_MESSAGE = "Compilation Failed"
  const val NOT_RUNNABLE_MESSAGE = "Solution isn't runnable"
  const val LOGIN_NEEDED_MESSAGE = "Please, login to Stepik to check the task"
  const val FAILED_TO_CHECK_MESSAGE = "Failed to launch checking"
  const val SYNTAX_ERROR_MESSAGE = "Syntax Error"
  val ERRORS = listOf(COMPILATION_FAILED_MESSAGE, FAILED_TO_CHECK_MESSAGE, SYNTAX_ERROR_MESSAGE)

  fun navigateToFailedPlaceholder(eduState: EduState, task: Task, taskDir: VirtualFile, project: Project) {
    val selectedTaskFile = eduState.taskFile ?: return
    var editor = eduState.editor
    var taskFileToNavigate = selectedTaskFile
    var fileToNavigate = eduState.virtualFile
    val studyTaskManager = StudyTaskManager.getInstance(project)
    if (!studyTaskManager.hasFailedAnswerPlaceholders(selectedTaskFile)) {
      for ((_, taskFile) in task.taskFiles) {
        if (studyTaskManager.hasFailedAnswerPlaceholders(taskFile)) {
          taskFileToNavigate = taskFile
          val virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
          val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile)
          if (fileEditor is EduSingleFileEditor) {
            editor = fileEditor.editor
          }
          fileToNavigate = virtualFile
          break
        }
      }
    }
    if (fileToNavigate != null) {
      FileEditorManager.getInstance(project).openFile(fileToNavigate, true)
    }
    if (editor == null) {
      return
    }
    ApplicationManager.getApplication().invokeLater {
      IdeFocusManager.getInstance(project).requestFocus(editor.contentComponent, true)
    }
    navigateToFirstFailedAnswerPlaceholder(editor, taskFileToNavigate)
  }

  fun flushWindows(task: Task, taskDir: VirtualFile) {
    for ((_, taskFile) in task.taskFiles) {
      val virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
      EduUtils.flushWindows(taskFile, virtualFile)
    }
  }

  fun createDefaultRunConfiguration(project: Project): RunnerAndConfigurationSettings? {
    return runReadAction {
      val editor = EduUtils.getSelectedEditor(project) ?: return@runReadAction null
      val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return@runReadAction null
      ConfigurationContext(psiFile).configuration
    }
  }

  fun hasCompilationErrors(processOutput: ProcessOutput): Boolean {
    for (error in COMPILATION_ERRORS) {
      if (processOutput.stderr.contains(error)) return true
    }
    return false
  }

  fun postProcessOutput(output: String): String {
    return output.replace(System.getProperty("line.separator"), "\n").removeSuffix("\n")
  }

  fun createRunConfiguration(project: Project, taskFile: VirtualFile?): RunnerAndConfigurationSettings? {
    return runReadActionInSmartMode(project) {
      val item = PsiUtilCore.findFileSystemItem(project, taskFile)
      if (item == null) null else ConfigurationContext(item).configuration
    }
  }

  fun executeRunConfigurations(
    project: Project,
    configurations: List<RunnerAndConfigurationSettings>,
    executionListener: ExecutionListener? = null,
    processListener: ProcessListener? = null,
    testEventsListener: SMTRunnerEventsListener? = null
  ) {
    val connection = project.messageBus.connect()
    try {
      executeRunConfigurations(connection, configurations, executionListener, processListener, testEventsListener)
    }
    finally {
      connection.disconnect()
    }
  }

  private fun executeRunConfigurations(
    connection: MessageBusConnection,
    configurations: List<RunnerAndConfigurationSettings>,
    executionListener: ExecutionListener?,
    processListener: ProcessListener?,
    testEventsListener: SMTRunnerEventsListener?
  ) {
    testEventsListener?.let { connection.subscribe(SMTRunnerEventsListener.TEST_STATUS, it) }
    val latch = CountDownLatch(configurations.size)

    runInEdt {
      val environments = mutableListOf<ExecutionEnvironment>()
      connection.subscribe(ExecutionManager.EXECUTION_TOPIC,
                           CheckExecutionListener(DefaultRunExecutor.EXECUTOR_ID, environments, latch, executionListener))

      for (configuration in configurations) {
        val runner = ProgramRunner.getRunner(DefaultRunExecutor.EXECUTOR_ID, configuration.configuration)
        val env = ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), configuration).activeTarget().build()
        environments.add(env)
        runner?.execute(env) { descriptor ->
          // Descriptor can be null in some cases.
          // For example, IntelliJ Rust's test runner provides null here if compilation fails
          if (descriptor == null) {
            latch.countDown()
            return@execute
          }

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
      }
    }

    latch.await()
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
