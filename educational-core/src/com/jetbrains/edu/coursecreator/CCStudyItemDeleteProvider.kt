package com.jetbrains.edu.coursecreator

import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.DeleteProvider
import com.intellij.ide.IdeBundle
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ui.configuration.actions.ModuleDeleteProvider
import com.intellij.openapi.ui.Messages.*
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.projectView.CourseViewPane
import java.util.*

class CCStudyItemDeleteProvider : DeleteProvider {

  override fun canDeleteElement(dataContext: DataContext): Boolean = dataContext.getData(CourseViewPane.STUDY_ITEM) != null

  override fun deleteElement(dataContext: DataContext) {
    val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return
    val virtualFile = dataContext.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
    val studyItem = dataContext.getData(CourseViewPane.STUDY_ITEM) ?: return
    // currently, only gradle projects have module for study items
    val module = dataContext.getData(LangDataKeys.MODULE_CONTEXT)
    val itemType = when (studyItem) {
      is Section -> EduCoreBundle.message("item.section.title")
      is Lesson -> EduCoreBundle.message("item.lesson.title")
      is Task -> EduCoreBundle.message("item.task.title")
      else -> return
    }

    val title = IdeBundle.message("prompt.delete.elements", itemType)
    val message = buildString {
      append(IdeBundle.message("warning.delete.all.files.and.subdirectories", studyItem.name))
    }

    val result = showOkCancelDialog(project, message, title, getOkButton(), getCancelButton(), getQuestionIcon())
    if (result != OK) return

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

  companion object {

    @VisibleForTesting
    fun taskMessageName(task: Task): String {
      val section = task.lesson.section?.name?.let { "$it#" } ?: ""
      return "$section${task.lesson.name}#${task.name}"
    }
  }
}
