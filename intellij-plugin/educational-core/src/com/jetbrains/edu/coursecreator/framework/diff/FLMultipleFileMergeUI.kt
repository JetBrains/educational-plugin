package com.jetbrains.edu.coursecreator.framework.diff

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.merge.MultipleFileMergeDialog
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JButton
import javax.swing.JComponent

interface FLMultipleFileMergeUI {
  fun show(
    project: Project,
    files: List<VirtualFile>,
    mergeProvider: FLMergeProvider,
    mergeCustomizer: FLMergeDialogCustomizer,
    currentTaskName: String,
    targetTaskName: String
  ): Boolean
}

class FLMultipleFileMergeUIImpl : FLMultipleFileMergeUI {
  override fun show(
    project: Project,
    files: List<VirtualFile>,
    mergeProvider: FLMergeProvider,
    mergeCustomizer: FLMergeDialogCustomizer,
    currentTaskName: String,
    targetTaskName: String
  ): Boolean {
    val multipleFileMergeDialog = createFLMultipleFileMergeDialog(project, files, mergeProvider, mergeCustomizer, currentTaskName, targetTaskName)
    multipleFileMergeDialog.show()
    // multipleFileMergeDialog always exits with cancel code
    return multipleFileMergeDialog.processedFiles.size == files.size
  }
}

fun createFLMultipleFileMergeDialog(
  project: Project,
  files: List<VirtualFile>,
  mergeProvider: FLMergeProvider,
  mergeDialogCustomizer: FLMergeDialogCustomizer,
  currentTaskName: String,
  targetTaskName: String,
): MultipleFileMergeDialog {
  return object : MultipleFileMergeDialog(project, files, mergeProvider, mergeDialogCustomizer) {
    override fun createCenterPanel(): JComponent {
      return super.createCenterPanel().apply {
        val buttons = components.filterIsInstance<JButton>()

        val acceptYoursButton = buttons.find { it.text == VcsBundle.message("multiple.file.merge.accept.yours") }
        val acceptTheirsButton = buttons.find { it.text == VcsBundle.message("multiple.file.merge.accept.theirs") }

        acceptYoursButton?.text = EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.MergeDialog.AcceptFromButton.text", currentTaskName)
        acceptTheirsButton?.text = EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.MergeDialog.AcceptFromButton.text", targetTaskName)
      }
    }
  }
}