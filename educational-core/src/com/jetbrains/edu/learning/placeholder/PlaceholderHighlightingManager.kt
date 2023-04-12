package com.jetbrains.edu.learning.placeholder

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile

object PlaceholderHighlightingManager {

  @JvmOverloads
  @JvmStatic
  fun showPlaceholders(project: Project, taskFile: TaskFile, editor: Editor? = null) {
    PlaceholderPainter.showPlaceholders(project, taskFile, editor)
  }

  @JvmStatic
  fun showPlaceholder(project: Project, placeholder: AnswerPlaceholder) {
    PlaceholderPainter.showPlaceholder(project, placeholder)
  }

  @JvmStatic
  fun hidePlaceholders(taskFile: TaskFile) {
    PlaceholderPainter.hidePlaceholders(taskFile)
  }

  @JvmStatic
  fun hidePlaceholder(placeholder: AnswerPlaceholder) {
    PlaceholderPainter.hidePlaceholder(placeholder)
  }
}
