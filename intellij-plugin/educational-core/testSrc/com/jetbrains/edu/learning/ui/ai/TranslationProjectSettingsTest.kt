package com.jetbrains.edu.learning.ui.ai

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.ai.TranslationProperties
import com.jetbrains.educational.core.enum.TranslationLanguage
import com.jetbrains.educational.translation.format.domain.TranslationVersion
import junit.framework.ComparisonFailure
import org.junit.Test

class TranslationProjectSettingsTest : EduSettingsServiceTestBase() {
  @Test
  fun `test serialization`() {
    val settings = TranslationProjectSettings()
    settings.loadStateAndCheck("""
      <TranslationProjectState>
        <option name="currentTranslationLanguage" value="Russian" />
        <option name="translationVersions">
          <map>
            <entry key="Russian" value="1" />
          </map>
        </option>
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
  fun `test settings serialization with language being set`() {
    val settings = TranslationProjectSettings()
    settings.setTranslation(TranslationProperties(TranslationLanguage.FRENCH, TranslationVersion(1)))
    settings.checkState("""
      <TranslationProjectState>
        <option name="currentTranslationLanguage" value="French" />
        <option name="translationVersions">
          <map>
            <entry key="French" value="1" />
          </map>
        </option>
      </TranslationProjectState>
    """)
  }

  @Test(expected = ComparisonFailure::class)
  fun `test settings serialization when language is set but no version`() {
    val settings = TranslationProjectSettings()
    settings.loadStateAndCheck("""
      <TranslationProjectState>
        <option name="currentTranslationLanguage" value="French" />
        <option name="translationVersions">
          <map>
            <entry key="Russian" value="1" />
          </map>
        </option>
      </TranslationProjectState>
    """)
  }
}