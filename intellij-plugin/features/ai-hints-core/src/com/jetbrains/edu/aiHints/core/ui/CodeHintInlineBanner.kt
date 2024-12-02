package com.jetbrains.edu.aiHints.core.ui

import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import org.jetbrains.annotations.Nls

class CodeHintInlineBanner(
  project: Project,
  message: @Nls String,
  private val highlighter: RangeHighlighter? = null
) : HintInlineBanner(project, message) {

  override fun removeNotify() {
    super.removeNotify()
    highlighter?.dispose()
  }

  fun addCodeHint(showInCodeAction: () -> Unit): CodeHintInlineBanner {
    addAction(EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.show.code.text")) {
      showInCodeAction()
    }
    return this
  }
}