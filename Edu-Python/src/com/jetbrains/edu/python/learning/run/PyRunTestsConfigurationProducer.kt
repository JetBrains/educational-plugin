package com.jetbrains.edu.python.learning.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.python.learning.PyConfigurator

class PyRunTestsConfigurationProducer : LazyRunConfigurationProducer<PyRunTestConfiguration>() {
  override fun setupConfigurationFromContext(
    configuration: PyRunTestConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement>
  ): Boolean {
    val testsPath = getTestPath(context) ?: return false
    val testsFile = LocalFileSystem.getInstance().findFileByPath(testsPath) ?: return false
    val generatedName = generateName(testsFile, context.project) ?: return false
    configuration.pathToTest = testsPath
    configuration.name = generatedName
    configuration.scriptName = testsFile.path
    return true
  }

  override fun getConfigurationFactory(): ConfigurationFactory {
    return runConfigurationType<PyRunTestsConfigurationType>().configurationFactories[0]
  }

  override fun isConfigurationFromContext(configuration: PyRunTestConfiguration, context: ConfigurationContext): Boolean {
    val path = getTestPath(context)
    return path != null && path == configuration.pathToTest
  }

  companion object {
    private fun generateName(testsFile: VirtualFile, project: Project): String? {
      val task = testsFile.getContainingTask(project) ?: return null
      return FileUtil.join(task.lesson.name, task.name)
    }

    private fun getTestPath(context: ConfigurationContext): String? {
      val location = context.location ?: return null
      val file = location.virtualFile ?: return null
      val project = location.project
      val task = file.getContainingTask(project) ?: return null
      val taskDir = task.getDir(project.courseDir) ?: return null
      val taskDirPath = FileUtil.toSystemDependentName(taskDir.path)
      val testsPath = if (taskDir.findChild(EduNames.SRC) != null) {
        FileUtil.join(taskDirPath, EduNames.SRC, PyConfigurator.TESTS_PY)
      }
      else {
        FileUtil.join(taskDirPath, PyConfigurator.TESTS_PY)
      }
      val filePath = FileUtil.toSystemDependentName(file.path)
      return if (filePath == testsPath) testsPath else null
    }
  }
}
