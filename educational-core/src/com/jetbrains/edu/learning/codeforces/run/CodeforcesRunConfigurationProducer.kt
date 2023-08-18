package com.jetbrains.edu.learning.codeforces.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.jetbrains.edu.learning.codeforces.CodeforcesUtils
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.ext.getCodeTaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.getContainingTask

class CodeforcesRunConfigurationProducer : LazyRunConfigurationProducer<CodeforcesRunConfiguration>() {
  override fun setupConfigurationFromContext(
    configuration: CodeforcesRunConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement>
  ): Boolean {
    if (configuration is EmptyCodeforcesRunConfiguration) return false

    val project = context.project
    val selectedFile = context.location?.virtualFile ?: return false
    val testsFile = CodeforcesUtils.getInputFile(project, selectedFile) ?: return false
    val task = selectedFile.getContainingTask(project) as? CodeforcesTask ?: return false
    val codeFile = task.getCodeTaskFile(project)?.getVirtualFile(project) ?: return false

    configuration.name = "${task.presentableName} (${testsFile.parent.name})"
    configuration.setExecutableFile(codeFile)
    configuration.inputRedirectOptions.isRedirectInput = true
    configuration.inputRedirectOptions.redirectInputPath = testsFile.path
    return true
  }

  override fun getConfigurationFactory(): ConfigurationFactory {
    return CodeforcesRunConfigurationType.getInstance().configurationFactories[0]
  }

  override fun isConfigurationFromContext(configuration: CodeforcesRunConfiguration, context: ConfigurationContext): Boolean {
    if (configuration is EmptyCodeforcesRunConfiguration) return false
    val selectedFiles = context.dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return false
    if (selectedFiles.size != 1) return false
    val selectedFile = selectedFiles.firstOrNull() ?: return false
    val testsFile = CodeforcesUtils.getInputFile(context.project, selectedFile) ?: return false
    return testsFile == configuration.getRedirectInputFile()
  }

  override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
    return true
  }
}
