package com.jetbrains.edu.java.learning.stepik.alt

import com.intellij.lang.DependentLanguage
import com.intellij.lang.Language
import com.jetbrains.edu.learning.EduNames

class JHyperskill : Language(JHYPERSKILL_LANGUAGE), DependentLanguage {
  override fun getDisplayName() = EduNames.JAVA
}
