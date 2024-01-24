package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import org.jetbrains.annotations.NonNls

/**
 * move caret to next answer placeholder
 */
class NextPlaceholderAction : PlaceholderNavigationAction() {
  override fun getTargetPlaceholder(taskFile: TaskFile, offset: Int): AnswerPlaceholder? {
    val selectedAnswerPlaceholder = taskFile.getAnswerPlaceholder(offset)
    val placeholders = taskFile.answerPlaceholders
    val startIndex = if (selectedAnswerPlaceholder != null) selectedAnswerPlaceholder.index + 1 else 0
    if (!indexIsValid(startIndex, placeholders)) return null
    for (placeholder in placeholders.subList(startIndex, placeholders.size)) {
      if (placeholder.offset > offset && placeholder.isCurrentlyVisible) {
        return placeholder
      }
    }
    return null
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  companion object {
    const val ACTION_ID: @NonNls String = "Educational.NextPlaceholder"
  }
}
