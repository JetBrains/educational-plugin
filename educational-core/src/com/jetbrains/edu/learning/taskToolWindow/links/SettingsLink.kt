package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.ide.actions.ShowSettingsUtilImpl
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.options.ex.ConfigurableExtensionPointUtil
import com.intellij.openapi.options.ex.ConfigurableVisitor
import com.intellij.openapi.project.Project

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class SettingsLink(link: String) : TaskDescriptionLink<String, String>(link) {
  override fun resolve(project: Project): String = linkPath

  override fun open(project: Project, configurableId: String) {
    runInEdt {
      ShowSettingsUtilImpl.showSettingsDialog(project, configurableId, null)
    }
  }

  override suspend fun validate(project: Project, configurableId: String): String? {
    val group = ConfigurableExtensionPointUtil.getConfigurableGroup(project, true)
    val configurable = ConfigurableVisitor.findById(configurableId, listOf(group))
    return if (configurable == null) "Failed to find settings page by `$configurableId` id" else null
  }
}
