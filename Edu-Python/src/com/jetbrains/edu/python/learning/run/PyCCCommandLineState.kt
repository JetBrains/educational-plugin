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
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.python.run.CommandLinePatcher
import com.jetbrains.python.run.PythonCommandLineState

class PyCCCommandLineState(
  private val runConfiguration: PyCCRunTestConfiguration,
  env: ExecutionEnvironment
) : PythonCommandLineState(runConfiguration, env) {

  private val project: Project = runConfiguration.project
  private val taskDir: VirtualFile
  private val task: Task

  init {
    val testsFile = LocalFileSystem.getInstance().findFileByPath(runConfiguration.pathToTest)
                    ?: error("Failed to find ${runConfiguration.pathToTest}")
    task = EduUtils.getTaskForFile(project, testsFile) ?: error("Failed to find task for `${testsFile.path}`")
    taskDir = task.getTaskDir(project) ?: error("Failed to get task dir for `${task.name}` task")
    consoleBuilder = PyCCConsoleBuilder(runConfiguration, env.executor)
  }

  private val currentTaskFilePath: String?
    get() {
      var taskFilePath: String? = null
      for ((_, taskFile) in task.taskFiles) {
        val file = EduUtils.findTaskFileInDir(taskFile, taskDir)
        if (file == null) {
          LOG.warn("Can't find virtual file for `${taskFile.name}` task file in `${task.name}` task")
          continue
        }
        if (EduUtils.isTestsFile(project, file)) continue
        if (!TextEditorProvider.isTextFile(file)) continue
        if (taskFilePath == null) {
          taskFilePath = file.systemDependentPath
        }

        // TODO: Come up with a smarter way how to find correct task file
        // Try to find task file with new placeholder. See https://youtrack.jetbrains.com/issue/EDU-1443
        val hasNewPlaceholder = taskFile.answerPlaceholders.any { p -> p.placeholderDependency == null }
        if (hasNewPlaceholder) return file.systemDependentPath
      }
      return taskFilePath
    }

  override fun buildCommandLineParameters(commandLine: GeneralCommandLine) {
    val project = runConfiguration.project
    check(StudyTaskManager.getInstance(project).course != null)

    commandLine.setWorkDirectory(taskDir.path)
    val group = commandLine.parametersList.getParamsGroup(GROUP_SCRIPT)!!
    group.addParameter(runConfiguration.pathToTest)
    val path = currentTaskFilePath
    if (path != null) {
      group.addParameter(path)
    }
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

  private val VirtualFile.systemDependentPath: String get() = FileUtil.toSystemDependentName(path)

  companion object {
    private val LOG: Logger = Logger.getInstance(PyCCCommandLineState::class.java)
  }
}
