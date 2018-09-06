package com.jetbrains.edu.android

import com.intellij.lang.DependentLanguage
import com.intellij.lang.Language
import com.jetbrains.edu.learning.EduNames

object Android : Language(EduNames.ANDROID), DependentLanguage {
  override fun getDisplayName(): String = "Android"
}
