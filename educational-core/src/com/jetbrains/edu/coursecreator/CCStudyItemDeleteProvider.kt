package com.jetbrains.edu.coursecreator

import com.intellij.ide.DeleteProvider
import com.intellij.ide.IdeBundle
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ui.configuration.actions.ModuleDeleteProvider
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.intellij.generation.EduGradleUtils
import com.jetbrains.edu.learning.projectView.CourseViewPane
import java.util.*

class CCStudyItemDeleteProvider : DeleteProvider {

  override fun canDeleteElement(dataContext: DataContext): Boolean = dataContext.getData(CourseViewPane.STUDY_ITEM) != null

  override fun deleteElement(dataContext: DataContext) {
    val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return
    val virtualFile = dataContext.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
    val studyItem = dataContext.getData(CourseViewPane.STUDY_ITEM) ?: return
    // currently, only gradle projects have module for lessons and tasks
    // so we can skip other projects
    val module = if (EduGradleUtils.isConfiguredWithGradle(project)) dataContext.getData(LangDataKeys.MODULE) else null
    val itemType = when (studyItem) {
      is Lesson -> "Lesson"
      is Task -> "Task"
      else -> return
    }

    val title = IdeBundle.message("prompt.delete.elements", itemType)
    val message = IdeBundle.message("warning.delete.all.files.and.subdirectories", studyItem.name)
    val result = Messages.showOkCancelDialog(message, title, Messages.getQuestionIcon())
    if (result != Messages.OK) return

    val modifiableModel = ModuleManager.getInstance(project).modifiableModel
    if (module != null) {
      ModuleDeleteProvider.removeModule(module, null, Collections.emptyList(), modifiableModel)
    }

    runWriteAction {
      modifiableModel.commit()
      CommandProcessor.getInstance().executeCommand(project, {
        virtualFile.delete(CCStudyItemDeleteProvider::class.java)
      }, "", Object())
    }
  }
}
