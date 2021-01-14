package com.jetbrains.edu.learning.codeforces.run

import com.google.common.annotations.VisibleForTesting
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.jetbrains.edu.learning.codeforces.CodeforcesUtils.isTestDataFolder
import com.jetbrains.edu.learning.codeforces.CodeforcesUtils.isValidCodeforcesTestFolder
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.ext.getCodeTaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.getContainingTask

class CodeforcesRunConfigurationProducer : LazyRunConfigurationProducer<CodeforcesRunConfiguration>() {
  override fun setupConfigurationFromContext(configuration: CodeforcesRunConfiguration,
                                             context: ConfigurationContext,
                                             sourceElement: Ref<PsiElement>): Boolean {
    if (configuration is InvalidCodeforcesRunConfiguration) return false

    val project = context.project
    val selectedFile = context.location?.virtualFile ?: return false
    val testsFile = getInputFile(project, selectedFile) ?: return false
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
    val selectedFiles = context.dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return false
    if (selectedFiles.size != 1) return false
    val selectedFile = selectedFiles.firstOrNull() ?: return false
    val testsFile = getInputFile(context.project, selectedFile) ?: return false
    return testsFile == configuration.getRedirectInputFile()
  }

  override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
    return true
  }

  companion object {
    @VisibleForTesting
    fun getInputFile(project: Project, selectedFile: VirtualFile): VirtualFile? {
      val task = selectedFile.getContainingTask(project) as? CodeforcesTask ?: return null
      return selectedFile.getTestFolder(project, task)?.findChild(task.inputFileName)
    }

    private fun VirtualFile.getTestFolder(project: Project, task: CodeforcesTask): VirtualFile? {
      var resultCandidate = this
      while (true) {
        if (resultCandidate.name == task.name) break // no need to go up more
        val testDataFolderCandidate = resultCandidate.parent ?: break
        if (testDataFolderCandidate.isTestDataFolder(project, task)) {
          // If it's not valid, we don't want to try another folders for creating configuration
          return if (resultCandidate.isValidCodeforcesTestFolder(task)) resultCandidate else null
        }
        resultCandidate = testDataFolderCandidate
      }

      val allTestFolders = task.getTestFolders(project)
      return if (allTestFolders.size == 1 && isTestDataFolder(project, task)) {
        allTestFolders.firstOrNull { it.isValidCodeforcesTestFolder(task) }
      }
      else {
        null
      }
    }
  }
}
