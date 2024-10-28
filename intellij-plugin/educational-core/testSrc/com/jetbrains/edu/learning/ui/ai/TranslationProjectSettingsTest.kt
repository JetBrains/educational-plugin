package com.jetbrains.edu.learning.ui.ai

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.educational.core.enum.Language
import org.junit.Test

class TranslationProjectSettingsTest : EduSettingsServiceTestBase() {
  @Test
  fun `test serialization`() {
    val settings = TranslationProjectSettings()
    settings.loadStateAndCheck("""
      <TranslationProjectState>
        <option name="currentTranslationLanguage" value="Russian" />
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
    settings.setCurrentTranslationLanguage(Language.FRENCH)
    settings.checkState("""
        <TranslationProjectState>
          <option name="currentTranslationLanguage" value="French" />
        </TranslationProjectState>
    """)
  }
}