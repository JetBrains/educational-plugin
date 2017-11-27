package com.jetbrains.edu.learning.intellij

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel

data class JdkProjectSettings(val model: ProjectSdksModel, val jdkItem: JdkComboBox.JdkComboBoxItem?) {
  companion object {
    fun emptySettings(): JdkProjectSettings {
      val configurable = ProjectStructureConfigurable.getInstance(ProjectManager.getInstance().defaultProject)
      return JdkProjectSettings(configurable.projectJdksModel, null)
    }
  }
}
