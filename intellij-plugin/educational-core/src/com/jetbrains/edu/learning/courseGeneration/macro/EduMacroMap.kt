package com.jetbrains.edu.learning.courseGeneration.macro

import com.intellij.openapi.components.PathMacroMap

@Suppress("EqualsOrHashCode")
class EduMacroMap(private val mode: SubstitutionMode, private val macros: List<EduMacro>) : PathMacroMap() {

  override fun hashCode(): Int = macros.hashCode()

  override fun substitute(text: String, caseSensitive: Boolean): String {
    var result: CharSequence = text
    for (macro in macros) {
      result = when (mode) {
        SubstitutionMode.COLLAPSE -> replace(result, macro.substitution, "$${macro.name}$", caseSensitive)
        SubstitutionMode.EXPAND -> replace(result, "$${macro.name}$", macro.substitution, caseSensitive)
      }
    }
    return result.toString()
  }

  private fun replace(text: CharSequence, pattern: String, substitution: String, caseSensitive: Boolean): CharSequence {
    if (text.length < pattern.length || pattern.isEmpty()) return text
    val newText = StringBuilder()
    var i = 0
    while (i < text.length) {
      val occurrence = text.indexOf(pattern, i, ignoreCase = !caseSensitive)
      if (occurrence < 0) {
        if (newText.isEmpty()) return text
        newText.append(text, i, text.length)
        break
      }
      else {
        newText.append(text, i, occurrence)
        newText.append(substitution)
        i = occurrence + pattern.length
      }
    }
    return newText
  }

  enum class SubstitutionMode {
    COLLAPSE,
    EXPAND
  }
}
