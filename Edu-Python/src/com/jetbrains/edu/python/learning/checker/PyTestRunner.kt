package com.jetbrains.edu.python.learning.checker

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager.findConfigurator
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.sdk.PythonSdkUtil
import java.io.File

internal class PyTestRunner(private val taskDir: VirtualFile) {
  fun createCheckProcess(project: Project, executablePath: String): Process? {
    val module = ModuleManager.getInstance(project).modules[0]
    val sdk = PythonSdkUtil.findPythonSdk(module)
    val configurator = findConfigurator(EduFormatNames.PYCHARM, DEFAULT_ENVIRONMENT, PythonLanguage.getInstance())
    if (configurator == null) {
      LOG.warn("Plugin configurator for Python is null")
      return null
    }
    val commandLine = GeneralCommandLine()
    commandLine.withWorkDirectory(taskDir.path)
    commandLine.environment[PYTHONPATH] = project.courseDir.path

    if (sdk != null) {
      val pythonPath = sdk.homePath ?: return null
      commandLine.exePath = pythonPath
      val testRunner = File(taskDir.path, configurator.testFileName)
      commandLine.addParameter(testRunner.path)
      commandLine.addParameter(FileUtil.toSystemDependentName(executablePath))
      return commandLine.createProcess()
    }
    return null
  }

  companion object {
    private val LOG = Logger.getInstance(PyTestRunner::class.java)
    private const val PYTHONPATH = "PYTHONPATH"
  }
}
