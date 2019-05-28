package com.jetbrains.edu.python.coursecreator.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduNames
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

  private val taskDir: VirtualFile
  private val task: Task

  init {
    val testsFile = LocalFileSystem.getInstance().findFileByPath(runConfiguration.pathToTest)!!
    val project = runConfiguration.project
    task = EduUtils.getTaskForFile(project, testsFile)!!
    taskDir = task.getTaskDir(project)!!
  }

  private val currentTaskFilePath: String?
    get() {
      var textFile: String? = null
      for ((key, value) in task.taskFiles) {
        val path = getTaskFilePath(key)
        if (value.answerPlaceholders.isNotEmpty()) {
          return path
        }
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(path) ?: continue
        if (TextEditorProvider.isTextFile(virtualFile)) {
          textFile = path
        }
      }
      return textFile
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

  private fun getTaskFilePath(name: String): String {
    val taskDirPath = FileUtil.toSystemDependentName(taskDir.path)
    return if (taskDir.findChild(EduNames.SRC) != null) {
      FileUtil.join(taskDirPath, EduNames.SRC, name)
    }
    else {
      FileUtil.join(taskDirPath, name)
    }
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
}
