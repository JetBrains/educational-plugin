package com.jetbrains.edu.learning.actions

import com.intellij.diff.chains.DiffRequestChain
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.contents.DocumentContentBase
import com.intellij.diff.editor.ChainDiffVirtualFile
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys.LAST_ACTIVE_FILE_EDITOR
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.DumbAwareActionButton
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls

class ApplyCodeAction : DumbAwareActionButton(
  EduCoreBundle.message("action.apply.code.from.submission.title"), EducationalCoreIcons.ApplyCode
) {

  override fun updateButton(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = project.isStudentProject() && e.isFileNamesPresented()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (!getConfirmationFromDialog(project)) return

    val diffRequestChain = e.getDiffRequestChain() ?: return showApplySubmissionCodeFailedNotification(project)
    val fileNames = diffRequestChain.getUserData(FILENAMES_KEY) ?: return showApplySubmissionCodeFailedNotification(project)

    try {
      val localDocuments = readLocalDocuments(fileNames)
      check(localDocuments.size == fileNames.size)
      val submissionsTexts = diffRequestChain.getSubmissionsText(fileNames.size)
      localDocuments.writeSubmissionsTexts(submissionsTexts)
    }
    catch (e: Exception) {
      showApplySubmissionCodeFailedNotification(project)
      return
    }

    project.closeDiffWindow(e)
    showApplySubmissionCodeSuccessfulNotification(project)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  private fun AnActionEvent.isFileNamesPresented() = getDiffRequestChain()?.getUserData(FILENAMES_KEY) != null

  private fun getConfirmationFromDialog(project: Project): Boolean = when (Messages.showYesNoDialog(
    project,
    EduCoreBundle.message("action.apply.code.from.submission.dialog.text"),
    EduCoreBundle.message("action.apply.code.from.submission.dialog.title"),
    EduCoreBundle.message("action.apply.code.from.submission.dialog.yes.text"),
    EduCoreBundle.message("action.apply.code.from.submission.dialog.no.text"),
    AllIcons.General.Warning
  )) {
    Messages.YES -> true
    else -> false
  }

  private fun AnActionEvent.getDiffRequestChain(): DiffRequestChain? {
    val chainDiffVirtualFile = getData(CommonDataKeys.VIRTUAL_FILE) as? ChainDiffVirtualFile
    return chainDiffVirtualFile?.chain
  }

  private fun readLocalDocuments(fileNames: List<String>): List<Document> = runReadAction {
    fileNames.mapNotNull { findLocalDocument(it) }
  }

  private fun findLocalDocument(fileName: String): Document? {
    val file = LocalFileSystem.getInstance().findFileByPath(fileName) ?: return null
    return FileDocumentManager.getInstance().getDocument(file)
  }

  private fun DiffRequestChain.getSubmissionsText(size: Int): List<String> {
    val diffRequestWrappers = List(size) { requests[it] as SimpleDiffRequestChain.DiffRequestProducerWrapper }
    val diffRequests = diffRequestWrappers.map { it.request as SimpleDiffRequest }
    return diffRequests.map { it.contents[1] as DocumentContentBase }.map { it.document.text }
  }

  private fun List<Document>.writeSubmissionsTexts(submissionsTexts: List<String>): Unit = runWriteAction {
    zip(submissionsTexts).forEach { (document, submissionText) ->
      document.setText(submissionText)
    }
  }

  private fun Project.closeDiffWindow(e: AnActionEvent) {
    val fileEditorManager = FileEditorManager.getInstance(this)
    val fileEditor = e.getData(LAST_ACTIVE_FILE_EDITOR) ?: return
    fileEditorManager.closeFile(fileEditor.file)
  }

  @Suppress("DialogTitleCapitalization")
  private fun showApplySubmissionCodeSuccessfulNotification(project: Project) = Notification(
    MarketplaceNotificationUtils.JETBRAINS_ACADEMY_GROUP_ID,
    EduCoreBundle.message("action.apply.code.from.submission.notification.success.title"),
    EduCoreBundle.message("action.apply.code.from.submission.notification.success.text"),
    NotificationType.INFORMATION
  ).notify(project)

  @Suppress("DialogTitleCapitalization")
  private fun showApplySubmissionCodeFailedNotification(project: Project) = Notification(
    MarketplaceNotificationUtils.JETBRAINS_ACADEMY_GROUP_ID,
    EduCoreBundle.message("action.apply.code.from.submission.notification.failed.title"),
    EduCoreBundle.message("action.apply.code.from.submission.notification.failed.text"),
    NotificationType.ERROR
  ).notify(project)

  companion object {
    val FILENAMES_KEY: Key<List<String>> = Key.create("fileNames")

    @NonNls
    const val ACTION_ID: String = "Educational.Student.ApplyCode"
  }
}