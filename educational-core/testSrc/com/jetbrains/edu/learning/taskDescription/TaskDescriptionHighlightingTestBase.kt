package com.jetbrains.edu.learning.taskDescription

abstract class TaskDescriptionHighlightingTestBase : TaskDescriptionTestBase() {

  override fun removeUnimportantParts(html: String): String {
    return super.removeUnimportantParts(html)
      .lines()
      .joinToString("\n", transform = String::trimEnd)
      .replace(SPAN_REGEX, """<span style="...">""")
  }

  companion object {
    private val SPAN_REGEX = Regex("""<span style=".*?">""")
  }
}
