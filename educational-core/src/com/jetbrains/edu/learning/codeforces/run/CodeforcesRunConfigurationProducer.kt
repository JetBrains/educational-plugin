package com.jetbrains.edu.learning.codeforces.run

import com.google.common.annotations.VisibleForTesting
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getCodeTaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.getContainingTask

class CodeforcesRunConfigurationProducer : LazyRunConfigurationProducer<CodeforcesRunConfiguration>() {
  override fun setupConfigurationFromContext(configuration: CodeforcesRunConfiguration,
                                             context: ConfigurationContext,
                                             sourceElement: Ref<PsiElement>): Boolean {
    val project = context.project
    val selectedFile = context.location?.virtualFile ?: return false
    val testsFile = getInputFile(project, selectedFile) ?: return false
    val task = selectedFile.getContainingTask(project) as? CodeforcesTask ?: return false
    val codeFile = task.getCodeTaskFile()?.getVirtualFile(project) ?: return false
    val codeExecutor = task.course.configurator?.taskCheckerProvider?.getCodeExecutor() ?: return false

    configuration.name = "${task.presentableName} (${testsFile.parent.name})"
    configuration.setExecutableFile(codeFile)
    codeExecutor.setInputRedirectFile(testsFile, configuration)
    return true
  }

  override fun getConfigurationFactory(): ConfigurationFactory {
    return CodeforcesRunConfigurationType.getInstance().configurationFactories[0]
  }

  override fun isConfigurationFromContext(configuration: CodeforcesRunConfiguration,
                                          context: ConfigurationContext): Boolean {
    val selectedFile = context.location?.virtualFile ?: return false
    val testsFile = getInputFile(context.project, selectedFile) ?: return false
    return testsFile == configuration.getRedirectInputFile()
  }

  companion object {
    @VisibleForTesting
    fun getInputFile(project: Project, selectedFile: VirtualFile): VirtualFile? {
      val task = selectedFile.getContainingTask(project) as? CodeforcesTask ?: return null
      val taskDir = task.getDir(project.courseDir) ?: return null
      val folderCandidate = getTestFolderCandidate(selectedFile, taskDir, task)
      val testFolder = if (folderCandidate != null) {
        // dependency to other review
        // if (folderCandidate.isValidCodeforcesTestFolder(task)) folderCandidate else null
        folderCandidate
      }
      else {
        val allTestFolders = task.getTestFolders(project)
        if (allTestFolders.size == 1) allTestFolders.firstOrNull() else null
      }
      return testFolder?.findChild(task.inputFileName)
    }

    private fun getTestFolderCandidate(selectedFile: VirtualFile, taskDir: VirtualFile, task: CodeforcesTask): VirtualFile? {
      var file = selectedFile
      while (true) {
        if (file.name == task.name) return null // no need to go up
        val testDataFolder = file.parent ?: return null
        if (testDataFolder.name == CodeforcesNames.TEST_DATA_FOLDER && testDataFolder.parent == taskDir) {
          return file
        }
        file = testDataFolder
      }
    }
  }
}
