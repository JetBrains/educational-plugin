package com.jetbrains.edu.learning.navigation

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.yaml.configFile

/**
 * Navigates to the YAML entry of an edu file inside the corresponding `task-info.yaml` or `course-info.yaml`.
 */
interface NavigateToEduFileExtension {
  /**
   * @return true if navigation was successful, false otherwise
   */
  fun navigateToEduFile(project: Project, eduFile: EduFile): Boolean

  companion object {
    val EP: ExtensionPointName<NavigateToEduFileExtension> = ExtensionPointName.create("Educational.navigateToEduFile")

    fun navigateToEduFile(project: Project, eduFile: EduFile): Boolean {
      val extension = EP.extensionList.firstOrNull()
      return extension?.navigateToEduFile(project, eduFile) ?: defaultNavigation(project, eduFile)
    }

    private fun defaultNavigation(project: Project, eduFile: EduFile): Boolean {
      val itemWithConfig = if (eduFile is TaskFile) {
        eduFile.task
      }
      else {
        project.course ?: return false
      }

      val configToOpen = itemWithConfig.configFile(project) ?: return false

      runInEdt {
        FileEditorManager.getInstance(project).openFile(configToOpen, true)
      }

      return true
    }
  }
}
