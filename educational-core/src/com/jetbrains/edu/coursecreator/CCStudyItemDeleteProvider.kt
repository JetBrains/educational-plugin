package com.jetbrains.edu.coursecreator

import com.intellij.ide.DeleteProvider
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ui.configuration.actions.ModuleDeleteProvider
import com.jetbrains.edu.learning.intellij.generation.EduGradleUtils
import com.jetbrains.edu.learning.projectView.CourseViewPane
import java.util.*

class CCStudyItemDeleteProvider : DeleteProvider {

  override fun canDeleteElement(dataContext: DataContext): Boolean = dataContext.getData(CourseViewPane.STUDY_ITEM) != null

  override fun deleteElement(dataContext: DataContext) {
    val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return
    val virtualFile = dataContext.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

    // currently, only gradle projects have module for lessons and tasks
    // so we can skip other projects
    val module = if (EduGradleUtils.isConfiguredWithGradle(project)) dataContext.getData(LangDataKeys.MODULE) else null
    val modifiableModel = ModuleManager.getInstance(project).modifiableModel
    if (module != null) {
      ModuleDeleteProvider.removeModule(module, Collections.emptyList(), modifiableModel)
    }

    runWriteAction {
      modifiableModel.commit()
      CommandProcessor.getInstance().executeCommand(project, {
        virtualFile.delete(CCStudyItemDeleteProvider::class.java)
      }, "", Object())
    }
  }
}
