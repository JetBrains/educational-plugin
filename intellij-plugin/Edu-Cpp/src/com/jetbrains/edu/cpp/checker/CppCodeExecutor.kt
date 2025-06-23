package com.jetbrains.edu.cpp.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childLeafs
import com.jetbrains.cidr.cpp.runfile.CppFileEntryPointDetector
import com.jetbrains.cidr.execution.CidrTargetRunConfigurationProducer
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class CppCodeExecutor : DefaultCodeExecutor() {

  override fun createRunConfiguration(project: Project, task: Task): RunnerAndConfigurationSettings? {
    val entryPoint = task.taskFiles
      .mapNotNull { (_, taskFile) ->
        val file = taskFile.getVirtualFile(project)
        if (file == null) {
          LOG.warn("Cannot get a virtual file from the task file '${taskFile.name}'")
        }
        file
      }.firstNotNullOfOrNull { file -> findEntryPointElement(project, file) }

    if (entryPoint == null) {
      LOG.warn("Failed to find main entry point for task '${task.name}'")
      return null
    }

    val mainElement = CppRunConfigurationHelper.getInstance()?.prepareEntryPointForRunConfiguration(entryPoint)
    if (mainElement == null) {
      LOG.warn("Failed to get wrapper for main psi element for file '${entryPoint.containingFile.name}'")
      return null
    }

    val context = ConfigurationContext(mainElement)
    val configuration = CidrTargetRunConfigurationProducer.getInstances(project)
      .firstOrNull { it.getExecutableTargetsForFile(entryPoint.containingFile).isNotEmpty() }
      ?.findOrCreateConfigurationFromContext(context)
    if (configuration == null) {
      LOG.warn("Failed to create a configuration from main function in the file '${entryPoint.containingFile.name}'")
      return null
    }
    return configuration.configurationSettings
  }

  private fun findEntryPointElement(project: Project, virtualFile: VirtualFile): PsiElement? {
    val psiFile = virtualFile.findPsiFile(project) ?: return null
    val entryPointDetector = CppFileEntryPointDetector.getInstance() ?: return null
    // TODO: is there more efficient way to do it than iterating over all leaf children without referring to particular psi classes?
    return psiFile.childLeafs().find { entryPointDetector.isMainOrIsInMain(it) }
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CppCodeExecutor::class.java)
  }
}