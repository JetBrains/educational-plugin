package com.jetbrains.edu.cpp.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.jetbrains.cidr.execution.CidrTargetRunConfigurationProducer
import com.jetbrains.cidr.execution.CidrTargetRunLineMarkerProvider
import com.jetbrains.cidr.lang.psi.OCFunctionDeclaration
import com.jetbrains.edu.learning.checker.OutputTaskChecker
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask

class CppOutputTaskChecker(task: OutputTask, project: Project) : OutputTaskChecker(task, project) {
  override fun createTestConfiguration(): RunnerAndConfigurationSettings? {
    return runReadAction {
      val mainFunction = task.taskFiles
        .mapNotNull { (_, taskFile) ->
          val file = taskFile.getVirtualFile(project)
          if (file == null) {
            LOG.warn("Cannot get a virtual file from the task file '${taskFile.name}'")
          }
          file
        }
        .mapNotNull { file -> findMainFunction(file) }
        .firstOrNull()

      if (mainFunction == null) {
        return@runReadAction null
      }

      val context = ConfigurationContext(mainFunction)

      val configuration = CidrTargetRunConfigurationProducer.getInstance(project)?.findOrCreateConfigurationFromContext(context)
      if (configuration == null) {
        LOG.warn("Failed to create a configuration from main function in the file '${mainFunction.containingFile.name}'")
        return@runReadAction null
      }

      return@runReadAction configuration.configurationSettings
    }
  }

  private fun findMainFunction(virtualFile: VirtualFile): PsiElement? {
    val psiFile = PsiUtilBase.getPsiFile(project, virtualFile)
    val functions = PsiTreeUtil.findChildrenOfType(psiFile, OCFunctionDeclaration::class.java)
    return functions.find { CidrTargetRunLineMarkerProvider.isInEntryPointBody(it) }
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CppOutputTaskChecker::class.java)
  }
}
