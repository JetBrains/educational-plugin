package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.intellij.ui.dsl.gridLayout.UnscaledGapsY
import javax.swing.JComponent
import org.apache.commons.text.StringEscapeUtils
import org.apache.commons.text.StringSubstitutor

// BACKCOMPAT 2023.1. Inline it.
fun <T: JComponent> Cell<T>.addGaps(top: Int = 0, left: Int = 0, bottom: Int = 0, right: Int = 0): Cell<T> {
  return customize(UnscaledGaps(top, left, bottom, right))
}

// BACKCOMPAT 2023.1. Inline it.
fun Row.addVerticalGaps(top: Int = 0, bottom: Int = 0): Row = customize(UnscaledGapsY(top, bottom))

// BACKCOMPAT: 2023.2. Inline it.
fun escapeHtml(s: String): String = StringEscapeUtils.escapeHtml4(s)

// BACKCOMPAT: 2023.2. Inline it.
fun replaceWithTemplateText(resources: Map<String, String>, templateText: String): String {
  return StringSubstitutor(resources).replace(templateText)
}