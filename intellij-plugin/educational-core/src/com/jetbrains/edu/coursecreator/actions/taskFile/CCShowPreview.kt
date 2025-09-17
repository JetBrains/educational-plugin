package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diff.impl.util.LabeledEditor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.FrameWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.PathUtil
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.TASK_FILE
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException
import com.jetbrains.edu.learning.exceptions.HugeBinaryFileException
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.placeholder.PlaceholderHighlightingManager.showPlaceholders
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.Companion.previewTaskFile
import com.jetbrains.edu.learning.toStudentFile
import org.jetbrains.annotations.NonNls
import java.awt.Dimension
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

class CCShowPreview : DumbAwareAction() {

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    if (!isCourseCreator(project)) {
      return
    }
    val file = CommonDataKeys.PSI_FILE.getData(e.dataContext) ?: return
    val taskFile = file.virtualFile.getTaskFile(project)
    if (taskFile != null && taskFile.isVisible) {
      presentation.isEnabledAndVisible = true
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val file = CommonDataKeys.PSI_FILE.getData(e.dataContext) ?: return
    StudyTaskManager.getInstance(project).course ?: return
    val virtualFile = file.virtualFile
    val taskFile = virtualFile.getTaskFile(project) ?: return
    if (!taskFile.isVisible) {
      return
    }
    val taskDir = file.containingDirectory ?: return
    taskDir.parentDirectory ?: return
    if (taskFile.answerPlaceholders.isEmpty()) {
      Messages.showInfoMessage(
        message("dialog.message.no.preview.for.file"),
        message("dialog.title.no.preview.for.file")
      )
      return
    }

    ApplicationManager.getApplication().runWriteAction {
      val studentTaskFile: TaskFile? = try {
        virtualFile.toStudentFile(project, taskFile.task, taskFile)
      }
      catch (exception: BrokenPlaceholderException) {
        LOG.info("Failed to Create Preview: " + exception.message)
        Messages.showErrorDialog(exception.placeholderInfo, message("dialog.title.failed.to.create.preview"))
        return@runWriteAction
      }
      catch (exception: HugeBinaryFileException) {
        LOG.info("Failed to Create Preview: " + exception.message)
        Messages.showErrorDialog(exception.message, message("dialog.title.failed.to.create.preview"))
        return@runWriteAction
      }
      if (studentTaskFile != null) {
        showPreviewDialog(project, studentTaskFile)
      }
    }
    previewTaskFile()
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  companion object {
    private val LOG = logger<CCShowPreview>()
    const val ACTION_ID: @NonNls String = "Educational.Educator.ShowPreview"

    private fun showPreviewDialog(project: Project, taskFile: TaskFile) {
      val showPreviewFrame = FrameWrapper(project)
      val userFile = LightVirtualFile(PathUtil.getFileName(taskFile.name), taskFile.text)
      showPreviewFrame.title = userFile.name
      userFile.putUserData(TASK_FILE, taskFile)
      val labeledEditor = LabeledEditor(null)
      val factory = EditorFactory.getInstance()
      val document = FileDocumentManager.getInstance().getDocument(userFile) ?: return
      val createdEditor = factory.createEditor(document, project, userFile, true) as EditorEx
      Disposer.register(StudyTaskManager.getInstance(project)) { factory.releaseEditor(createdEditor) }
      showPlaceholders(project, taskFile, createdEditor)
      val header = JPanel()
      header.layout = BoxLayout(header, BoxLayout.Y_AXIS)
      header.border = JBUI.Borders.empty(10)
      header.add(JLabel(message("ui.label.read.only.preview")))
      val timeStamp = SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().time)
      header.add(JLabel(message("ui.label.created.at", timeStamp)))
      val editorComponent = createdEditor.component
      labeledEditor.setComponent(editorComponent, header)
      createdEditor.setCaretVisible(false)
      createdEditor.setCaretEnabled(false)
      showPreviewFrame.component = labeledEditor
      if (!ApplicationManager.getApplication().isUnitTestMode) {
        showPreviewFrame.setSize(Dimension(500, 500))
        showPreviewFrame.show()
      }
    }
  }
}
