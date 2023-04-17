package com.jetbrains.edu.shell

import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings

class ShellLanguageSettings : LanguageSettings<EmptyProjectSettings>() {
  override fun getSettings(): EmptyProjectSettings = EmptyProjectSettings
}