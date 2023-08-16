package com.jetbrains.edu.learning.editor

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ReadOnlyFragmentModificationException
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler

/**
 * Used to forbid placeholder deletion while executing line actions
 */
class EduTypedLineHandler(private val originalHandler: EditorActionHandler) : EditorWriteActionHandler(false) {
  override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext): Boolean = true

  override fun executeWriteAction(editor: Editor, caret: Caret?, dataContext: DataContext) {
    val currentCaret = editor.caretModel.primaryCaret
    val taskFile = getTaskFile(editor)
    if (taskFile == null) {
      originalHandler.execute(editor, caret, dataContext)
      return
    }
    val document = editor.document
    val lineNumber = document.getLineNumber(currentCaret.offset)
    val lineEndOffset = document.getLineEndOffset(lineNumber)
    val lineStartOffset = document.getLineStartOffset(lineNumber)
    val placeholder = getAnswerPlaceholder(lineStartOffset, lineEndOffset, taskFile.answerPlaceholders)
    if (placeholder != null) throw ReadOnlyFragmentModificationException(null, null)
    originalHandler.execute(editor, caret, dataContext)
  }
}
