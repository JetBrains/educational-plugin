package com.jetbrains.edu.ai.translation.settings

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import org.junit.Test

class TranslationSettingsTest : EduSettingsServiceTestBase() {
  @Test
  fun `test serialization`() {
    val settings = TranslationSettings()
    settings.loadStateAndCheck(
      """
        <State>
          <option name="autoTranslate" value="true" />
          <option name="preferableLanguage" value="Russian" />
        </State>
    """
    )
  }
}