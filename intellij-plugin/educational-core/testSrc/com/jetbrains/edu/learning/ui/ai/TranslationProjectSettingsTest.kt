package com.jetbrains.edu.learning.ui.ai

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import org.junit.Test

class TranslationProjectSettingsTest : EduSettingsServiceTestBase() {
  @Test
  fun `test serialization`() {
    val settings = TranslationProjectSettings()
    settings.loadStateAndCheck("""
      <TranslationProjectState>
        <option name="currentTranslationLanguageCode" value="ru" />
      </TranslationProjectState>
    """)
  }

  @Test
  fun `test no settings serialization`() {
    val settings = TranslationProjectSettings()
    settings.checkState("""
        <TranslationProjectState />
    """)
  }

  @Test
  fun `test settings with selected language serialization`() {
    val settings = TranslationProjectSettings()
    settings.currentTranslationLanguageCode = "fr"
    settings.checkState("""
        <TranslationProjectState>
          <option name="currentTranslationLanguageCode" value="fr" />
        </TranslationProjectState>
    """)
  }
}