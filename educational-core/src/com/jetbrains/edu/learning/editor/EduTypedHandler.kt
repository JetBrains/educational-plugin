package com.jetbrains.edu.learning.editor

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ReadOnlyFragmentModificationException
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.getTaskFile

/**
 * Used to forbid placeholder deletion
 */
open class EduTypedHandler(@JvmField protected val originalHandler: EditorActionHandler) : EditorWriteActionHandler(false) {
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

  companion object {
    @OptIn(ExperimentalStdlibApi::class)
    fun getAnswerPlaceholder(start: Int, end: Int, placeholders: List<AnswerPlaceholder>): AnswerPlaceholder? {
      for (placeholder in placeholders) {
        val placeholderStart = placeholder.offset
        val placeholderEnd = placeholder.endOffset
        if (placeholderStart == start && placeholderEnd == end) continue
        if (placeholderStart in start..<end && placeholderEnd <= end && placeholderEnd > start) {
          return placeholder
        }
      }
      return null
    }

    @JvmStatic
    fun getTaskFile(editor: Editor?): TaskFile? {
      if (editor == null) return null
      val project = editor.project ?: return null
      val openedFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
      return openedFile.getTaskFile(project)
    }
  }
}
