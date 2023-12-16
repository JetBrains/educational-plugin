package com.jetbrains.edu.learning.editor

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ReadOnlyFragmentModificationException
import com.intellij.openapi.editor.actionSystem.EditorActionHandler

class EduCutTypedHandler(originalHandler: EditorActionHandler) : EduTypedHandler(originalHandler) {
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
    val document = editor.document
    val lineNumber = document.getLineNumber(currentCaret.offset)
    val lineEndOffset = document.getLineEndOffset(lineNumber)
    val lineStartOffset = document.getLineStartOffset(lineNumber)
    placeholder = getAnswerPlaceholder(lineStartOffset, lineEndOffset, taskFile.answerPlaceholders)
    if (placeholder != null && start == end) {
      throw ReadOnlyFragmentModificationException(null, null)
    }
    else {
      originalHandler.execute(editor, caret, dataContext)
    }
  }
}
