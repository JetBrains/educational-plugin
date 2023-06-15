package com.jetbrains.edu.learning.editor

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ReadOnlyFragmentModificationException
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler

/**
 * Used to forbid placeholder deletion
 */
open class EduTypedHandler(protected val originalHandler: EditorActionHandler) : EditorWriteActionHandler(false) {
  override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext): Boolean = true

  override fun executeWriteAction(editor: Editor, caret: Caret?, dataContext: DataContext) {
    val currentCaret = editor.caretModel.primaryCaret
    val taskFile = getTaskFile(editor)
    if (taskFile == null) {
      originalHandler.execute(editor, caret, dataContext)
      return
    }
    val start = editor.selectionModel.selectionStart
    val end = editor.selectionModel.selectionEnd
    var placeholder = getAnswerPlaceholder(start, end, taskFile.answerPlaceholders)
    if (placeholder != null && editor.selectionModel.hasSelection()) {
      throw ReadOnlyFragmentModificationException(null, null)
    }
    placeholder = taskFile.getAnswerPlaceholder(currentCaret.offset)
    if (placeholder != null && placeholder.length == 0) {
      throw ReadOnlyFragmentModificationException(null, null)
    }
    else {
      originalHandler.execute(editor, caret, dataContext)
    }
  }
}
