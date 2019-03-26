package com.jetbrains.edu.cpp

import com.jetbrains.edu.learning.LanguageSettings

class CppLanguageSettings : LanguageSettings<CppProjectSettings>() {
  override fun getSettings(): CppProjectSettings = CppProjectSettings()
}