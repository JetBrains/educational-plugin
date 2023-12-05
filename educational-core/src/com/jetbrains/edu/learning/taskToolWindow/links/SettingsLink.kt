package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.ide.actions.ShowSettingsUtilImpl
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class SettingsLink(link: String) : TaskDescriptionLink<String>(link) {
  override fun resolve(project: Project): String = linkPath

  override fun open(project: Project, configurableId: String) {
    runInEdt {
      ShowSettingsUtilImpl.showSettingsDialog(project, configurableId, null)
    }
  }
}