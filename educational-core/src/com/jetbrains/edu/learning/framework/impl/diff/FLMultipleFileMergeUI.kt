package com.jetbrains.edu.learning.framework.impl.diff

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

class FLMultipleFileMergeUIImpl: FLMultipleFileMergeUI {
  override fun show(
    project: Project,
    files: List<VirtualFile>,
    mergeProvider: FLMergeProvider,
    mergeCustomizer: FLMergeDialogCustomizer,
    currentTaskName: String,
    targetTaskName: String
  ): Boolean {
    val multipleFileMergeDialog = FLMultipleFileMergeDialog(project, files, mergeProvider, mergeCustomizer, currentTaskName, targetTaskName)
    multipleFileMergeDialog.show()
    // multipleFileMergeDialog always exits with cancel code
    return multipleFileMergeDialog.processedFiles.size == files.size
  }
}

class FLMultipleFileMergeDialog(
  project: Project,
  files: List<VirtualFile>,
  mergeProvider: FLMergeProvider,
  mergeDialogCustomizer: FLMergeDialogCustomizer,
  private val currentTaskName: String,
  private val targetTaskName: String,
) : MultipleFileMergeDialog(project, files, mergeProvider, mergeDialogCustomizer) {
  private var acceptYoursButton: JButton? = null
  private var acceptTheirsButton: JButton? = null

  override fun createCenterPanel(): JComponent {
    return super.createCenterPanel().apply {
      val buttons = components.filterIsInstance<JButton>()
      acceptYoursButton = buttons.find { it.text == VcsBundle.message("multiple.file.merge.accept.yours") }
      acceptTheirsButton = buttons.find { it.text == VcsBundle.message("multiple.file.merge.accept.theirs") }
    }
  }

  override fun beforeShowCallback() {
    super.beforeShowCallback()
    acceptYoursButton?.text = EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.MergeDialog.AcceptFromButton.text", currentTaskName)
    acceptTheirsButton?.text = EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.MergeDialog.AcceptFromButton.text", targetTaskName)
  }
}