package com.jetbrains.edu.learning.editor

import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.labels.ActionLink
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduDocumentListener
import com.jetbrains.edu.learning.actions.RevertTaskAction
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.placeholderDependencies.PlaceholderDependencyManager
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Implementation of [EduEditor] which has panel with special buttons and task text
 *
 * @see [EduFileEditorProvider]
 */
class EduSingleFileEditor(
  project: Project,
  file: VirtualFile,
  override var taskFile: TaskFile
) : PsiAwareTextEditorImpl(project, file, TextEditorProvider.getInstance()), EduEditor {

  private val documentListener: EduDocumentListener = EduDocumentListener(taskFile, true)

  init {
    editor.document.addDocumentListener(documentListener)
    validateTaskFile()
  }

  override fun getState(level: FileEditorStateLevel): EduEditorState {
    val state = super.getState(level)
    return EduEditorState(state, null)
  }

  override fun setState(state: FileEditorState, exactState: Boolean) {
    val realState = (state as? EduEditorState)?.mainEditorState ?: state
    super<PsiAwareTextEditorImpl>.setState(realState, exactState)
  }

  override fun validateTaskFile() {
    if (!taskFile.isValid(editor.document.text)) {
      val panel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
      panel.add(JLabel(BROKEN_SOLUTION_ERROR_TEXT_START))
      val actionLink = ActionLink(ACTION_TEXT, RevertTaskAction())
      actionLink.verticalAlignment = SwingConstants.CENTER
      panel.add(actionLink)
      panel.add(JLabel(BROKEN_SOLUTION_ERROR_TEXT_END))
      panel.border = BorderFactory.createEmptyBorder(JBUI.scale(5), JBUI.scale(5), JBUI.scale(5), 0)
      editor.headerComponent = panel
    } else {
      editor.headerComponent = null
    }
  }

  override fun startLoading() {
    (editor as EditorEx).isViewer = true
    @Suppress("INACCESSIBLE_TYPE")
    val component = component as JBLoadingPanel
    component.setLoadingText("Loading solution")
    component.startLoading()
  }

  override fun stopLoading() {
    @Suppress("INACCESSIBLE_TYPE")
    (component as JBLoadingPanel).stopLoading()
    (editor as EditorEx).isViewer = false
  }

  override fun selectNotify() {
    super.selectNotify()
    PlaceholderDependencyManager.updateDependentPlaceholders(myProject, taskFile.task)
  }

  override fun dispose() {
    editor.document.removeDocumentListener(documentListener)
    super.dispose()
  }

  companion object {
    const val BROKEN_SOLUTION_ERROR_TEXT_START = "Solution can't be loaded."
    const val BROKEN_SOLUTION_ERROR_TEXT_END = " to solve it again"
    const val ACTION_TEXT = "Reset task"
  }
}
