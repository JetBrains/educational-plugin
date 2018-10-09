package com.jetbrains.edu.java.learning.stepik.alt

import com.intellij.lang.DependentLanguage
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage

class JHyperskill : Language(JavaLanguage.INSTANCE, JHYPERSKILL_LANGUAGE), DependentLanguage {
  override fun getDisplayName() = baseLanguage?.displayName ?: id
}
