package com.jetbrains.edu.learning.courseFormat

import com.intellij.lang.Language
import com.jetbrains.edu.learning.languageColors.LanguageColorManager
import java.awt.Color

private val DEFAULT_COLOR: Color = Color(70, 130, 180, 70)

interface Tag {
  val text: String
  val color: Color
}

class GeneralTag(override val text: String, override val color: Color) : Tag {
  constructor(text: String) : this(text, DEFAULT_COLOR)
}

class LanguageTag(language: Language) : Tag {
  override val text: String = language.displayName
  override val color: Color = LanguageColorManager[language] ?: DEFAULT_COLOR
}
