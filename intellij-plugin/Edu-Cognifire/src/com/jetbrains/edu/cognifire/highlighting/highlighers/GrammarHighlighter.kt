package com.jetbrains.edu.cognifire.highlighting.highlighers

import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.selectedEditor

class GrammarHighlighter(
  private val project: Project,
  private val startOffset: Int,
  private val endOffset: Int): ProdeHighlighter {
  override val attributes = EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES)

  override var markupHighlighter: RangeHighlighter? = null

  override fun addMarkupHighlighter(): RangeHighlighter? =
    project.selectedEditor?.markupModel?.addRangeHighlighter(
      startOffset,
      endOffset,
      HighlighterLayer.ERROR,
      attributes,
      HighlighterTargetArea.EXACT_RANGE
    ).also {
      markupHighlighter = it
    }
}