package com.jetbrains.edu.decomposition.feedback

import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.jetbrains.edu.decomposition.model.FunctionModel
import com.jetbrains.edu.learning.selectedEditor

object GranularityFeedbackProvider {
  fun provideFeedback(functionModels: List<FunctionModel>, project: Project) {
    val editor = project.selectedEditor ?: return
    editor.markupModel.removeAllHighlighters()
    functionModels.forEach { functionModel ->
      editor.markupModel.addLineHighlighter(
        editor.document.getLineNumber(functionModel.offset),
        HighlighterLayer.SELECTION - 1,
        TextAttributes().apply { backgroundColor = JBColor.YELLOW }
      )
    }
  }
}
