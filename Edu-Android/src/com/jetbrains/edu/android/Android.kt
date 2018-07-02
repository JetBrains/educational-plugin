package com.jetbrains.edu.android

import com.intellij.lang.DependentLanguage
import com.intellij.lang.Language

object Android : Language("edu-android"), DependentLanguage {
  override fun getDisplayName(): String = "Android"
}
