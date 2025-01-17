package com.jetbrains.edu.cognifire.highlighting.highlighers

import com.intellij.openapi.editor.markup.*
import com.intellij.ui.JBColor


class UncommittedChangesHighlighter(
  private val startOffset: Int,
  private val endOffset: Int
) : ProdeHighlighter {
  override val attributes = TextAttributes().apply { backgroundColor = JBColor(0xE6FFE6, 0x224422) }

  override var markupHighlighter: RangeHighlighter? = null

  override fun addMarkupHighlighter(markupModel: MarkupModel?): RangeHighlighter? =
    markupModel?.addRangeHighlighter(
      startOffset,
      endOffset,
      HighlighterLayer.SELECTION - 1,
      attributes,
      HighlighterTargetArea.EXACT_RANGE
    )?.also {
      markupHighlighter = it
    }
}