package com.jetbrains.edu.cognifire.highlighting.highlighers

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor

class UncommitedChangesHighlighter(
  private val editor: Editor,
  private val startOffset: Int,
  private val endOffset: Int
): ProdeHighlighter {
  override val attributes = TextAttributes().apply { backgroundColor = JBColor.green }

  override var markupHighlighter: RangeHighlighter? = null

  override fun addMarkupHighlighter(): RangeHighlighter =
    editor.markupModel.addRangeHighlighter(
      startOffset,
      endOffset,
      HighlighterLayer.SELECTION,
      attributes,
      HighlighterTargetArea.EXACT_RANGE
    ).also {
      markupHighlighter = it
    }

  fun addHighlighter() {
    markupHighlighter?.let {
      it.apply {
        editor.markupModel.addRangeHighlighter(
          startOffset,
          endOffset,
          HighlighterLayer.SELECTION,
          attributes,
          HighlighterTargetArea.EXACT_RANGE
        )
      }
    }
  }
}