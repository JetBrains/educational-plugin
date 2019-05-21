package com.jetbrains.edu.scala.sbt

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.courseFormat.ext.findTestDirs
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class ScalaSbtEduTaskChecker(task: EduTask, project: Project) : EduTaskCheckerBase(task, project) {
  override fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    return task.getAllTests(project).mapNotNull {
      val configuration = ConfigurationContext(it).configuration?.apply {
        isActivateToolWindowBeforeRun = false
        isTemporary = true
      }
      configuration
    }
  }

  private fun Task.getAllTests(project: Project): List<PsiDirectory> {
    val taskDir = getDir(project) ?: error("Failed to find dir for task $name")
    val testDirs = task.findTestDirs(taskDir)
    return testDirs.mapNotNull { PsiManager.getInstance(project).findDirectory(it) }
  }
}

