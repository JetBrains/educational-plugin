package com.jetbrains.edu.python.learning.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.python.learning.getCurrentTaskFilePath
import com.jetbrains.python.run.CommandLinePatcher
import com.jetbrains.python.run.PythonCommandLineState

class PyCCCommandLineState private constructor(
  private val runConfiguration: PyCCRunTestConfiguration,
  env: ExecutionEnvironment,
  private val task: Task,
  private val taskDir: VirtualFile
) : PythonCommandLineState(runConfiguration, env) {
  init {
    consoleBuilder = PyCCConsoleBuilder(runConfiguration, env.executor)
  }

  override fun buildCommandLineParameters(commandLine: GeneralCommandLine) {
    val project = runConfiguration.project
    check(StudyTaskManager.getInstance(project).course != null)

    commandLine.setWorkDirectory(taskDir.path)
    val group = commandLine.parametersList.getParamsGroup(GROUP_SCRIPT)!!
    group.addParameter(runConfiguration.pathToTest)
    val path = task.getCurrentTaskFilePath(project) ?: return
    group.addParameter(path)
  }

  @Throws(ExecutionException::class)
  override fun createAndAttachConsole(project: Project, processHandler: ProcessHandler, executor: Executor): ConsoleView {
    val console = createConsole(executor)!!

    // Filters are copied from PythonCommandLineState#createAndAttachConsole
    console.addMessageFilter(createUrlFilter(processHandler))
    addTracebackFilter(project, console, processHandler)

    console.attachToProcess(processHandler)
    return console
  }

  @Throws(ExecutionException::class)
  override fun execute(executor: Executor, processStarter: PythonProcessStarter, vararg patchers: CommandLinePatcher): ExecutionResult {
    runWriteAction { CheckUtils.flushWindows(task, taskDir) }
    return super.execute(executor, processStarter, *patchers)
  }

  @Throws(ExecutionException::class)
  override fun doCreateProcess(commandLine: GeneralCommandLine): ProcessHandler {
    val handler = super.doCreateProcess(commandLine)
    handler.addProcessListener(object : ProcessAdapter() {
      override fun processTerminated(event: ProcessEvent) {
        ApplicationManager.getApplication().invokeLater { EduUtils.deleteWindowDescriptions(task, taskDir) }
      }
    })
    return handler
  }

  companion object {
    private val LOG = Logger.getInstance(PyCCCommandLineState::class.java)

    @JvmStatic
    fun createInstance(configuration: PyCCRunTestConfiguration, environment: ExecutionEnvironment): PyCCCommandLineState? {
      fun logAndQuit(error: String): PyCCCommandLineState? {
        LOG.warn(error)
        return null
      }

      val testsFile = LocalFileSystem.getInstance().findFileByPath(configuration.pathToTest)
                      ?: return logAndQuit("Failed to find ${configuration.pathToTest}")

      val task = EduUtils.getTaskForFile(configuration.project, testsFile)
                 ?: return logAndQuit("Failed to find task for `${testsFile.path}`")

      val taskDir = task.getDir(configuration.project.courseDir) ?: return logAndQuit("Failed to get task dir for `${task.name}` task")

      if (configuration.sdk == null) return logAndQuit("Python SDK should not be null while creating instance of PyCCCommandLineState")
      return PyCCCommandLineState(configuration, environment, task, taskDir)
    }
  }
}
