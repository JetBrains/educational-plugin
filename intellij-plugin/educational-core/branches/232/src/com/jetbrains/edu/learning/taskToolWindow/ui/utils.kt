package com.jetbrains.edu.learning.taskToolWindow.ui

import org.apache.commons.lang.StringEscapeUtils
import org.apache.commons.lang.text.StrSubstitutor

// BACKCOMPAT: 2023.2. Inline it.
fun escapeHtml(s: String): String = StringEscapeUtils.escapeHtml(s)

// BACKCOMPAT: 2023.2. Inline it.
fun replaceWithTemplateText(resources: Map<String, String>, templateText: String): String {
  return StrSubstitutor(resources).replace(templateText)
}