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
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.GotItTooltip
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JComponent

class ApplyCodeAction : DumbAwareAction(), CustomComponentAction {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = project.isStudentProject() && e.isUserDataPresented()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (!getConfirmationFromDialog(project)) return

    val diffRequestChain = e.getDiffRequestChain() ?: return showApplyCodeFailedNotification(project)
    val fileNames = diffRequestChain.getUserData(VIRTUAL_FILE_PATH_LIST) ?: return showApplyCodeFailedNotification(project)

    try {
      val localDocuments = readLocalDocuments(fileNames)
      check(localDocuments.size == fileNames.size)
      val submissionsTexts = diffRequestChain.getTexts(fileNames.size)
      val runnableCommand = {
        localDocuments.writeTexts(submissionsTexts)
      }
      CommandProcessor.getInstance().executeCommand(project, runnableCommand, this.templatePresentation.text, ACTION_ID)
    }
    catch (e: Exception) {
      showApplyCodeFailedNotification(project)
      return
    }

    project.closeDiffWindow(e)
    showApplyCodeSuccessfulNotification(project)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent = object : ActionButton(
    this, presentation, place, JBUI.size(COMPONENT_SIZE)
  ) {
    init {
      val gotItTooltip = GotItTooltip(
        GOT_IT_ID, EduCoreBundle.message("action.Educational.Student.ApplyCode.tooltip.text")
      ).withHeader(EduCoreBundle.message("action.Educational.Student.ApplyCode.tooltip.title"))

      this.addComponentListener(object : ComponentAdapter() {
        override fun componentMoved(e: ComponentEvent) {
          val jComponent = e.component as JComponent
          if (jComponent.visibleRect.isEmpty) {
            gotItTooltip.hidePopup()
          }
          else {
            gotItTooltip.show(jComponent, GotItTooltip.BOTTOM_MIDDLE)
          }
        }
      })
    }
  }

  private fun AnActionEvent.isUserDataPresented() = getDiffRequestChain()?.getUserData(VIRTUAL_FILE_PATH_LIST) != null

  private fun getConfirmationFromDialog(project: Project): Boolean = when (Messages.showYesNoDialog(
    project,
    EduCoreBundle.message("action.Educational.Student.ApplyCode.dialog.text"),
    EduCoreBundle.message("action.Educational.Student.ApplyCode.dialog.title"),
    EduCoreBundle.message("action.Educational.Student.ApplyCode.dialog.yes.text"),
    EduCoreBundle.message("action.Educational.Student.ApplyCode.dialog.no.text"),
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

  private fun DiffRequestChain.getTexts(size: Int): List<String> {
    val diffRequestWrappers = List(size) { requests[it] as SimpleDiffRequestChain.DiffRequestProducerWrapper }
    val diffRequests = diffRequestWrappers.map { it.request as SimpleDiffRequest }
    return diffRequests.map { it.contents[1] as DocumentContentBase }.map { it.document.text }
  }

  private fun List<Document>.writeTexts(texts: List<String>): Unit = runWriteAction {
    zip(texts).forEach { (document, text) ->
      document.setText(text)
    }
  }

  private fun Project.closeDiffWindow(e: AnActionEvent) {
    val fileEditorManager = FileEditorManager.getInstance(this)
    val fileEditor = e.getData(LAST_ACTIVE_FILE_EDITOR) ?: return
    fileEditorManager.closeFile(fileEditor.file)
  }

  @Suppress("DialogTitleCapitalization")
  private fun showApplyCodeSuccessfulNotification(project: Project) = Notification(
    MarketplaceNotificationUtils.JETBRAINS_ACADEMY_GROUP_ID,
    EduCoreBundle.message("action.Educational.Student.ApplyCode.notification.success.title"),
    EduCoreBundle.message("action.Educational.Student.ApplyCode.notification.success.text"),
    NotificationType.INFORMATION
  ).notify(project)

  @Suppress("DialogTitleCapitalization")
  private fun showApplyCodeFailedNotification(project: Project) = Notification(
    MarketplaceNotificationUtils.JETBRAINS_ACADEMY_GROUP_ID,
    EduCoreBundle.message("action.Educational.Student.ApplyCode.notification.failed.title"),
    EduCoreBundle.message("action.Educational.Student.ApplyCode.notification.failed.text"),
    NotificationType.ERROR
  ).notify(project)

  companion object {
    val VIRTUAL_FILE_PATH_LIST: Key<List<String>> = Key.create("virtualFilePathList")

    @NonNls
    const val ACTION_ID: String = "Educational.Student.ApplyCode"

    @NonNls
    private const val GOT_IT_ID: String = "diff.toolbar.apply.code.action.button"

    private const val COMPONENT_SIZE: Int = 22
  }
}