package com.jetbrains.edu.coursecreator

import com.intellij.ide.DeleteProvider
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.jetbrains.edu.learning.projectView.CourseViewPane

class CCStudyItemDeleteProvider : DeleteProvider {

  override fun canDeleteElement(dataContext: DataContext): Boolean = dataContext.getData(CourseViewPane.STUDY_ITEM) != null

  override fun deleteElement(dataContext: DataContext) {
    val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return
    val virtualFile = dataContext.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

    runWriteAction {
      CommandProcessor.getInstance().executeCommand(project, {
        virtualFile.delete(CCStudyItemDeleteProvider::class.java)
      }, "", Object())
    }
  }
}
