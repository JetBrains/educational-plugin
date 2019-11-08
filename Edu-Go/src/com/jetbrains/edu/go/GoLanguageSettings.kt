package com.jetbrains.edu.go

import com.goide.GoLanguage
import com.jetbrains.edu.learning.LanguageSettings

class GoLanguageSettings: LanguageSettings<GoProjectSettings>() {
  override fun getSettings(): GoProjectSettings = GoProjectSettings(GoLanguage.INSTANCE)
}
