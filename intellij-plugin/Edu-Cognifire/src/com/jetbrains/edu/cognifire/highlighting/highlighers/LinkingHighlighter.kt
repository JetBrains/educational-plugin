package com.jetbrains.edu.cognifire.highlighting.highlighers

import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.jetbrains.edu.learning.selectedEditor

class LinkingHighlighter(
  private val project: Project,
  private val lineNumber: Int
) : ProdeHighlighter {
  override val attributes = TextAttributes().apply { backgroundColor = JBColor.LIGHT_GRAY }
  override var markupHighlighter: RangeHighlighter? = null
  override fun addMarkupHighlighter(): RangeHighlighter? =
    project.selectedEditor?.markupModel?.addLineHighlighter(
      lineNumber,
      HighlighterLayer.LAST,
      attributes
    ).also {
      markupHighlighter = it
    }
}