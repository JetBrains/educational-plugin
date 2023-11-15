package com.jetbrains.edu.learning.framework.impl.diff

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vcs.merge.MergeDialogCustomizer
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.pathRelativeToTask

class FLMergeDialogCustomizer(
  private val project: Project,
  private val currentTaskName: String,
  private val targetTaskName: String,
): MergeDialogCustomizer() {
  override fun getColumnNames(): List<String> {
    return listOf(currentTaskName, targetTaskName)
  }

  override fun getMergeWindowTitle(file: VirtualFile): String {
    return EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.MergeDialog.MergeWindow.title", file.pathRelativeToTask(project))
  }

  override fun getMultipleFileDialogTitle(): String {
    return EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.MergeDialog.MultipleFileDialog.title")
  }

  override fun getMultipleFileMergeDescription(files: MutableCollection<VirtualFile>): String {
    return EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.MergeDialog.MultipleFileDialog.description")
  }

  override fun getLeftPanelTitle(file: VirtualFile): String {
    return EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.MergeDialog.LeftPanel.title", currentTaskName)
  }

  override fun getCenterPanelTitle(file: VirtualFile): String {
    return EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.MergeDialog.CenterPanel.title")
  }

  override fun getRightPanelTitle(file: VirtualFile, revisionNumber: VcsRevisionNumber?): String {
    return EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.MergeDialog.RightPanel.title", targetTaskName)
  }
}