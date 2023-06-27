package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduUtilsKt.replaceAnswerPlaceholder
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.selectedEditor
import org.jetbrains.annotations.NonNls

class RefreshAnswerPlaceholder : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val placeholder = getAnswerPlaceholder(e) ?: return
    val editor = e.project?.selectedEditor ?: return
    replaceAnswerPlaceholder(editor.document, placeholder)
    placeholder.reset(false)
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return
    if (!course.isStudy) {
      presentation.isVisible = true
      return
    }
    if (getAnswerPlaceholder(e) != null) {
      presentation.isEnabledAndVisible = true
    }
  }

  companion object {
    const val ACTION_ID: @NonNls String = "Educational.RefreshAnswerPlaceholder"
    private fun getAnswerPlaceholder(e: AnActionEvent): AnswerPlaceholder? {
      val (_, editor, taskFile) = e.project?.eduState ?: return null
      return taskFile.getAnswerPlaceholder(editor.caretModel.offset)
    }
  }
}
