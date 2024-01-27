package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.util.DocumentUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.actions.EduActionUtils.runUndoableAction
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency.Companion.create
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.placeholder.PlaceholderHighlightingManager

open class CCAddAnswerPlaceholder : CCAnswerPlaceholderAction() {
  private fun addPlaceholder(state: EduState) {
    val editor = state.editor
    FileDocumentManager.getInstance().saveDocument(editor.document)
    val model = editor.selectionModel
    val offset = if (model.hasSelection()) model.selectionStart else editor.caretModel.offset
    val taskFile = state.taskFile
    val defaultPlaceholderText = defaultPlaceholderText(state.project)

    val answerPlaceholder = AnswerPlaceholder().apply {
      index = taskFile.answerPlaceholders.size
      this.taskFile = taskFile
      this.offset = offset
      placeholderText = defaultPlaceholderText
    }

    val dlg = createDialog(state.project, answerPlaceholder)
    if (!dlg.showAndGet()) {
      return
    }
    val answerPlaceholderText = dlg.getPlaceholderText()
    val selectedText = model.selectedText
    val possibleAnswer = if (model.hasSelection() && selectedText != null) selectedText else defaultPlaceholderText
    answerPlaceholder.placeholderText = answerPlaceholderText
    answerPlaceholder.length = possibleAnswer.length
    val dependencyInfo = dlg.getDependencyInfo()
    if (dependencyInfo != null) {
      answerPlaceholder.placeholderDependency = create(answerPlaceholder, dependencyInfo.dependencyPath)
    }
    answerPlaceholder.isVisible = dlg.getVisible()
    answerPlaceholder.init()
    if (!model.hasSelection()) {
      DocumentUtil.writeInRunUndoTransparentAction { editor.document.insertString(offset, defaultPlaceholderText) }
    }
    val action = AddAction(state.project, answerPlaceholder, taskFile, editor)
    runUndoableAction(state.project, message("action.Educational.Educator.AddAnswerPlaceholder.text"), action)
  }

  open fun createDialog(project: Project, answerPlaceholder: AnswerPlaceholder): CCCreateAnswerPlaceholderDialog {
    return CCCreateAnswerPlaceholderDialog(project, false, answerPlaceholder)
  }

  internal open class AddAction(
    project: Project,
    private val placeholder: AnswerPlaceholder,
    taskFile: TaskFile,
    editor: Editor
  ) : TaskFileUndoableAction(project, taskFile, editor) {
    override fun performUndo(): Boolean {
      if (taskFile.answerPlaceholders.contains(placeholder)) {
        taskFile.removeAnswerPlaceholder(placeholder)
        taskFile.sortAnswerPlaceholders()
        PlaceholderHighlightingManager.hidePlaceholder(project, placeholder)
        return true
      }
      return false
    }

    override fun performRedo() {
      taskFile.addAnswerPlaceholder(placeholder)
      taskFile.sortAnswerPlaceholders()
      PlaceholderHighlightingManager.showPlaceholder(project, placeholder)
    }
  }

  override fun performAnswerPlaceholderAction(state: EduState) {
    addPlaceholder(state)
  }

  override fun updatePresentation(eduState: EduState, presentation: Presentation) {
    presentation.isVisible = true
    if (canAddPlaceholder(eduState)) {
      presentation.isEnabled = true
    }
  }

  companion object {
    private fun arePlaceholdersIntersect(taskFile: TaskFile, start: Int, end: Int): Boolean {
      val answerPlaceholders = taskFile.answerPlaceholders
      for (existingAnswerPlaceholder in answerPlaceholders) {
        val twStart = existingAnswerPlaceholder.offset
        val twEnd = existingAnswerPlaceholder.possibleAnswer.length + twStart
        if (start in twStart until twEnd || end in (twStart + 1)..twEnd ||
            twStart in start until end || twEnd in (start + 1)..end) {
          return true
        }
      }
      return false
    }

    private fun defaultPlaceholderText(project: Project): String {
      val course = StudyTaskManager.getInstance(project).course ?: return CCUtils.DEFAULT_PLACEHOLDER_TEXT
      val configurator = course.configurator ?: return CCUtils.DEFAULT_PLACEHOLDER_TEXT
      return configurator.defaultPlaceholderText
    }

    private fun canAddPlaceholder(state: EduState): Boolean {
      val editor = state.editor
      val selectionModel = editor.selectionModel
      val taskFile = state.taskFile
      if (selectionModel.hasSelection()) {
        val start = selectionModel.selectionStart
        val end = selectionModel.selectionEnd
        return !arePlaceholdersIntersect(taskFile, start, end)
      }
      val offset = editor.caretModel.offset
      return taskFile.getAnswerPlaceholder(offset) == null
    }
  }
}
