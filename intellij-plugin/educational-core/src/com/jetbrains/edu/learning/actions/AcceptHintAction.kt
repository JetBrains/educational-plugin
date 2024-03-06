package com.jetbrains.edu.learning.actions

import com.intellij.diff.chains.DiffRequestChain
import com.intellij.diff.editor.ChainDiffVirtualFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.ui.JButtonAction
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.CancelHintAction.Companion.closeDiffWindow
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.getSubmissionsText
import org.jetbrains.annotations.NonNls
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.swing.JButton

// TODO: possibly reuse ApplyCodeAction class
class AcceptHintAction : JButtonAction(EduCoreBundle.message("action.Educational.Assistant.AcceptHint.button")) {
  val logger = KotlinLogging.logger("EduAssistantLogger")

  override fun getActionUpdateThread() = ActionUpdateThread.EDT

  override fun createButton(): JButton =
    object : JButton(templatePresentation.text) {
      override fun isDefaultButton(): Boolean = true
      override fun isEnabled(): Boolean = true
      override fun isFocusable(): Boolean = true
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = project.isStudentProject() && e.isNextStepHintDiff()
  }

  override fun actionPerformed(e: AnActionEvent) {
    logger.info { "User response: accepted code hint" }

    val project = e.project ?: return

    val diffRequestChain = e.getDiffRequestChain() ?: return
    val fileNames = diffRequestChain.getUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST).takeIf { !it.isNullOrEmpty() } ?: return

    try {
      val localDocuments = readLocalDocuments(fileNames)
      check(localDocuments.size == fileNames.size)
      val submissionsTexts = diffRequestChain.getSubmissionsText(fileNames.size)
      localDocuments.writeSubmissionsTexts(submissionsTexts)
    }
    catch (e: Exception) {
      return
    }
    diffRequestChain.putUserData(NextStepHintAction.IS_ACCEPTED_HINT, true)
    project.closeDiffWindow(e)
  }

  private fun readLocalDocuments(fileNames: List<String>): List<Document> = runReadAction {
    fileNames.mapNotNull { findLocalDocument(it) }
  }

  private fun findLocalDocument(fileName: String): Document? {
    val file = LocalFileSystem.getInstance().findFileByPath(fileName) ?: return null
    return FileDocumentManager.getInstance().getDocument(file)
  }

  private fun List<Document>.writeSubmissionsTexts(submissionsTexts: List<String>): Unit = runWriteAction {
    zip(submissionsTexts).forEach { (document, submissionText) ->
      document.setText(submissionText)
    }
  }

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.Assistant.AcceptHint"

    fun AnActionEvent.isNextStepHintDiff() = getDiffRequestChain()?.getUserData(NextStepHintAction.NEXT_STEP_HINT_DIFF_FLAG) == true

    fun AnActionEvent.getDiffRequestChain(): DiffRequestChain? {
      val chainDiffVirtualFile = getData(CommonDataKeys.VIRTUAL_FILE) as? ChainDiffVirtualFile
      return chainDiffVirtualFile?.chain
    }
  }
}
