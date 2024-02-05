package com.jetbrains.edu.coursecreator.framework.diff

import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vcs.merge.MergeDialogCustomizer
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.messages.EduCoreBundle

class FLMergeDialogCustomizer(
  private val currentTaskName: String,
  private val targetTaskName: String,
) : MergeDialogCustomizer() {
  override fun getColumnNames(): List<String> {
    return listOf(currentTaskName, targetTaskName)
  }

  override fun getMergeWindowTitle(file: VirtualFile): String {
    return EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.MergeDialog.MergeWindow.title", file.path)
  }

  override fun getMultipleFileDialogTitle(): String {
    return EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.MergeDialog.MultipleFileDialog.title")
  }

  override fun getMultipleFileMergeDescription(files: MutableCollection<VirtualFile>): String {
    return EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.MergeDialog.MultipleFileDialog.description")
  }

  override fun getLeftPanelTitle(file: VirtualFile): String {
    return EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.MergeDialog.LeftPanel.title", currentTaskName)
  }

  override fun getCenterPanelTitle(file: VirtualFile): String {
    return EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.MergeDialog.CenterPanel.title")
  }

  override fun getRightPanelTitle(file: VirtualFile, revisionNumber: VcsRevisionNumber?): String {
    return EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.MergeDialog.RightPanel.title", targetTaskName)
  }
}