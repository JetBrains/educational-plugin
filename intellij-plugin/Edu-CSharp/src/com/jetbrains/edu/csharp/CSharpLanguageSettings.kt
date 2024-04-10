package com.jetbrains.edu.csharp

import com.jetbrains.edu.learning.LanguageSettings

class CSharpLanguageSettings : LanguageSettings<CSharpProjectSettings>() {
  override fun getSettings(): CSharpProjectSettings = CSharpProjectSettings()
}