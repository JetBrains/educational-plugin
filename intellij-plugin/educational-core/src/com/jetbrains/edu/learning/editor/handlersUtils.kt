package com.jetbrains.edu.learning.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.getTaskFile

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

fun getTaskFile(editor: Editor?): TaskFile? {
  if (editor == null) return null
  val project = editor.project ?: return null
  val openedFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
  return openedFile.getTaskFile(project)
}