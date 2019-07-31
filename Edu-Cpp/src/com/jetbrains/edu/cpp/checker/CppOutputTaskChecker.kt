package com.jetbrains.edu.cpp.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
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
      val tasksDir = task.getDir(project)?.let { task.findSourceDir(it) } ?: return@runReadAction null
      val configurations = mutableListOf<ConfigurationFromContext>()

      VfsUtilCore.processFilesRecursively(tasksDir) {
        if (it == null || it.isDirectory) return@processFilesRecursively true

        val mainFunction = findMainFunction(it) ?: return@processFilesRecursively true

        val config = CidrTargetRunConfigurationProducer.getInstance(project)
          ?.findOrCreateConfigurationFromContext(ConfigurationContext(mainFunction))
        if (config == null) {
          LOG.warn("Failed to create configuration from main function in the file '${it.name}'")
          return@processFilesRecursively true
        }

        configurations += config

        true
      }

      configurations.firstOrNull()?.configurationSettings
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
