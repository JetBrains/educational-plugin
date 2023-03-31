package com.jetbrains.edu.python.learning.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.target.TargetEnvironment
import com.intellij.execution.target.TargetEnvironmentRequest
import com.intellij.execution.target.value.constant
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.python.learning.getCurrentTaskFilePath
import com.jetbrains.python.run.PythonCommandLineState
import com.jetbrains.python.run.PythonExecution
import com.jetbrains.python.run.PythonScriptExecution
import com.jetbrains.python.run.target.HelpersAwareTargetEnvironmentRequest
import java.util.function.Function

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

  @Suppress("UnstableApiUsage")
  override fun buildPythonExecution(helpersAwareRequest: HelpersAwareTargetEnvironmentRequest): PythonExecution {
    val pythonScriptExecution = PythonScriptExecution()
    pythonScriptExecution.pythonScriptPath = constant(runConfiguration.pathToTest)
    val project = runConfiguration.project
    val path = task.getCurrentTaskFilePath(project)
    if (path == null) {
      LOG.warn("Path to task file is null for a task ${task.name}")
      return pythonScriptExecution
    }
    pythonScriptExecution.addParameter(path)
    return pythonScriptExecution
  }

  @Suppress("UnstableApiUsage")
  override fun getPythonExecutionWorkingDir(targetEnvironmentRequest: TargetEnvironmentRequest): Function<TargetEnvironment, String> {
    return constant(taskDir.path)
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

      val task = testsFile.getContainingTask(configuration.project)
                 ?: return logAndQuit("Failed to find task for `${testsFile.path}`")

      val taskDir = task.getDir(configuration.project.courseDir) ?: return logAndQuit("Failed to get task dir for `${task.name}` task")

      if (configuration.sdk == null) return logAndQuit("Python SDK should not be null while creating instance of PyCCCommandLineState")
      return PyCCCommandLineState(configuration, environment, task, taskDir)
    }
  }
}
