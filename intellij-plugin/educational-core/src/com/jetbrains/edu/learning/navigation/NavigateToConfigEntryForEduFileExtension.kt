package com.jetbrains.edu.learning.navigation

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.yaml.configFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Navigates to the YAML entry of an edu file inside the corresponding `task-info.yaml` or `course-info.yaml`.
 */
interface NavigateToConfigEntryForEduFileExtension {
  /**
   * @return the config entry to navigate to or null if the element is not found or navigation is impossible
   */
  @RequiresReadLock
  fun findConfigEntry(configFile: PsiFile, eduFile: EduFile): NavigatablePsiElement?

  companion object {
    val EP: ExtensionPointName<NavigateToConfigEntryForEduFileExtension> = ExtensionPointName.create("Educational.navigateToConfigEntryForEduFile")

    suspend fun navigateToConfigEntryForEduFile(project: Project, eduFile: EduFile): Boolean {
      val extension = EP.extensionList.firstOrNull()

      val navigatableElement = readAction {
        val configFile = findConfigFile(project, eduFile) ?: return@readAction null
        extension?.findConfigEntry(configFile, eduFile) ?: configFile
      } ?: return false

      val canNavigate = readAction { navigatableElement.canNavigate() }

      if (canNavigate) {
        withContext(Dispatchers.EDT) {
          navigatableElement.navigate(true)
        }
      }

      return canNavigate
    }

    @RequiresReadLock
    private fun findConfigFile(project: Project, eduFile: EduFile): PsiFile? {
      val itemWithConfig = if (eduFile is TaskFile) {
        eduFile.task
      }
      else {
        project.course ?: return null
      }

      val configToOpen = itemWithConfig.configFile(project) ?: return null

      return configToOpen.findPsiFile(project)
    }
  }
}
