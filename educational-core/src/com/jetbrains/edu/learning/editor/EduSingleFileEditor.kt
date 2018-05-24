package com.jetbrains.edu.learning.editor

import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.labels.ActionLink
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.actions.RevertTaskAction
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.placeholderDependencies.PlaceholderDependencyManager
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Implementation of StudyEditor which has panel with special buttons and task text
 * also @see [EduFileEditorProvider]
 */
class EduSingleFileEditor(project: Project, file: VirtualFile) : PsiAwareTextEditorImpl(project, file, TextEditorProvider.getInstance()) {

  val taskFile: TaskFile? = EduUtils.getTaskFile(project, file)

  init {
    validateTaskFile()
  }

  fun validateTaskFile() {
    if (taskFile?.isValid(editor.document.text) == true) {
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

  fun showLoadingPanel() {
    (editor as EditorEx).isViewer = true
    @Suppress("INACCESSIBLE_TYPE")
    val component = component as JBLoadingPanel
    component.setLoadingText("Loading solution")
    component.startLoading()
  }

  override fun selectNotify() {
    super.selectNotify()
    PlaceholderDependencyManager.updateDependentPlaceholders(myProject, taskFile!!.task)
  }

  companion object {
    const val BROKEN_SOLUTION_ERROR_TEXT_START = "Solution can't be loaded."
    const val BROKEN_SOLUTION_ERROR_TEXT_END = " to solve it again"
    const val ACTION_TEXT = "Reset task"
  }
}
