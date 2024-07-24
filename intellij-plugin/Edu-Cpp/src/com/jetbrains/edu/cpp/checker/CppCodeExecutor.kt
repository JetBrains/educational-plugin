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
      return null
    }

    val context = ConfigurationContext(entryPoint)

    val configuration = CidrTargetRunConfigurationProducer.getInstances(project).firstOrNull()?.findOrCreateConfigurationFromContext(context)
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
    // It still doesn't work with new C++ language engine because
    // `CppFileNovaEntryPointDetector#isMainOrIsInMain` is not properly implemented yet.
    // See https://youtrack.jetbrains.com/issue/EDU-6773
    return psiFile.childLeafs().find { entryPointDetector.isMainOrIsInMain(it) }
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CppCodeExecutor::class.java)
  }
}