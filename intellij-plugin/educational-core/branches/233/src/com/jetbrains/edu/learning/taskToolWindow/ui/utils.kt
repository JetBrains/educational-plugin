package com.jetbrains.edu.learning.taskToolWindow.ui

import org.apache.commons.text.StringEscapeUtils
import org.apache.commons.text.StringSubstitutor

// BACKCOMPAT: 2023.2. Inline it.
fun escapeHtml(s: String): String = StringEscapeUtils.escapeHtml4(s)

// BACKCOMPAT: 2023.2. Inline it.
fun replaceWithTemplateText(resources: Map<String, String>, templateText: String): String {
  return StringSubstitutor(resources).replace(templateText)
}