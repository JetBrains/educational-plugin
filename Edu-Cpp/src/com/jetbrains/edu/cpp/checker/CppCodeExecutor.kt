package com.jetbrains.edu.cpp.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.jetbrains.cidr.execution.CidrTargetRunConfigurationProducer
import com.jetbrains.cidr.execution.OCTargetConfigurationHelper.isInEntryPointBody
import com.jetbrains.cidr.lang.psi.OCFunctionDeclaration
import com.jetbrains.edu.cpp.codeforces.CppCodeforcesRunConfiguration
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class CppCodeExecutor : DefaultCodeExecutor() {

  override fun execute(project: Project, task: Task, indicator: ProgressIndicator, input: String?): Result<String, CheckResult> {
    // BACKCOMPAT 2023.1 Get rid of it
    return withoutTerminalEmulation { super.execute(project, task, indicator, input) }
  }

  override fun createRunConfiguration(project: Project, task: Task): RunnerAndConfigurationSettings? {
    val mainFunction = task.taskFiles
      .mapNotNull { (_, taskFile) ->
        val file = taskFile.getVirtualFile(project)
        if (file == null) {
          LOG.warn("Cannot get a virtual file from the task file '${taskFile.name}'")
        }
        file
      }.firstNotNullOfOrNull { file -> findMainFunction(file, project) }

    if (mainFunction == null) {
      return null
    }

    val context = ConfigurationContext(mainFunction)

    val configuration = CidrTargetRunConfigurationProducer.getInstances(project).firstOrNull()?.findOrCreateConfigurationFromContext(context)
    if (configuration == null) {
      LOG.warn("Failed to create a configuration from main function in the file '${mainFunction.containingFile.name}'")
      return null
    }
    return configuration.configurationSettings
  }

  override fun createCodeforcesConfiguration(project: Project, factory: ConfigurationFactory): CodeforcesRunConfiguration {
    return CppCodeforcesRunConfiguration(project, factory)
  }

  private fun findMainFunction(virtualFile: VirtualFile, project: Project): PsiElement? {
    val psiFile = PsiUtilBase.getPsiFile(project, virtualFile)
    val functions = PsiTreeUtil.findChildrenOfType(psiFile, OCFunctionDeclaration::class.java)
    return functions.find { isInEntryPointBody(it) }
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CppCodeExecutor::class.java)
  }
}