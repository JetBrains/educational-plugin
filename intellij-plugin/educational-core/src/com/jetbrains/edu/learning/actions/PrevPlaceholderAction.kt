package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import org.jetbrains.annotations.NonNls

class PrevPlaceholderAction : PlaceholderNavigationAction() {
  override fun getTargetPlaceholder(taskFile: TaskFile, offset: Int): AnswerPlaceholder? {
    val selectedAnswerPlaceholder = taskFile.getAnswerPlaceholder(offset)
    val placeholders = taskFile.answerPlaceholders
    val endIndex = selectedAnswerPlaceholder?.index ?: placeholders.size
    if (!indexIsValid(endIndex - 1, placeholders)) return null
    for (placeholder in placeholders.subList(0, endIndex).asReversed()) {
      if (placeholder.offset < offset && placeholder.isCurrentlyVisible) {
        return placeholder
      }
    }
    return null
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  companion object {
    const val ACTION_ID: @NonNls String = "Educational.PrevPlaceholder"
  }
}
