package com.jetbrains.edu.javascript.learning

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiUtilCore
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class JsEduTaskChecker(task: EduTask, project: Project) : EduTaskCheckerBase(task, project) {

  override fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    return task.getAllTests(project).mapNotNull {
      val configuration = ConfigurationContext(it).configuration?.apply {
        isActivateToolWindowBeforeRun = !task.course.isStudy
        isTemporary = true
      }
      configuration
    }
  }

  private fun Task.getAllTests(project: Project): List<PsiFile> {
    val taskDir = getDir(project) ?: error("Failed to find dir for task $name")
    val testFiles = mutableListOf<VirtualFile>()

    VfsUtilCore.processFilesRecursively(taskDir) {
      if (EduUtils.isTestsFile(project, it)) {
        val psiFile = PsiManager.getInstance(project).findFile(it)
        if (psiFile != null) {
          testFiles.add(it)
        }
      }
      true
    }
    return PsiUtilCore.toPsiFiles(PsiManager.getInstance(project), testFiles)
  }

  override val SMTestProxy.comparisonMessage: String get() {
    // It is tested only with Jest so may not work with other JS test frameworks
    val index = StringUtil.indexOfIgnoreCase(errorMessage, "Expected:", 0)
    return if (index != -1) errorMessage.substring(0, index).trim() else errorMessage
  }
}
