package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.actions.EduActionUtils.updateAction
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.navigation.NavigationUtils.navigateToAnswerPlaceholder
import com.jetbrains.edu.learning.selectedEditor

abstract class PlaceholderNavigationAction : DumbAwareAction() {
  private fun navigateToPlaceholder(project: Project) {
    val selectedEditor = project.selectedEditor ?: return
    val openedFile = FileDocumentManager.getInstance().getFile(selectedEditor.document) ?: return
    val selectedTaskFile = openedFile.getTaskFile(project) ?: return
    val offset = selectedEditor.caretModel.offset
    val targetPlaceholder = getTargetPlaceholder(selectedTaskFile, offset) ?: return
    navigateToAnswerPlaceholder(selectedEditor, targetPlaceholder)
  }

  protected abstract fun getTargetPlaceholder(taskFile: TaskFile, offset: Int): AnswerPlaceholder?

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    navigateToPlaceholder(project)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    updateAction(e)
  }

  protected fun indexIsValid(index: Int, collection: Collection<*>): Boolean = index >= 0 && index < collection.size
}
