package com.jetbrains.edu.python.learning

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.python.PythonLanguage

class InstallDependenciesWorkaroundAction : DumbAwareAction() {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!project.isEduProject()) return
    val course = project.course ?: return
    if (course.languageById != PythonLanguage.INSTANCE) return

    e.presentation.isEnabledAndVisible = true
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return

    val projectSdk = ProjectRootManager.getInstance(project).projectSdk ?: return
    installRequiredPackages(project, projectSdk)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}