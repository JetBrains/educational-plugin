package com.jetbrains.edu.learning.ui.ai

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.ai.TranslationProperties
import com.jetbrains.educational.core.enum.TranslationLanguage
import junit.framework.ComparisonFailure
import org.junit.Test

class TranslationProjectSettingsTest : EduSettingsServiceTestBase() {
  @Test
  fun `test serialization`() {
    val settings = TranslationProjectSettings()
    settings.loadStateAndCheck("""
      <TranslationProjectState>
        <option name="currentTranslationLanguage" value="Russian" />
        <option name="structureTranslation">
          <map>
            <entry key="Russian">
              <value>
                <map>
                  <entry key="1" value="привет" />
                  <entry key="2" value="круасан" />
                  <entry key="3" value="два" />
                </map>
              </value>
            </entry>
            <entry key="French">
              <value>
                <map>
                  <entry key="1" value="bonjour" />
                  <entry key="2" value="croissant" />
                  <entry key="3" value="deux" />
                </map>
              </value>
            </entry>
          </map>
        </option>
        <option name="translationVersions">
          <map>
            <entry key="Russian" value="1" />
            <entry key="French" value="2" />
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
  fun `test settings serialization with translation properties being set`() {
    val translationProperties = TranslationProperties(
      language = TranslationLanguage.FRENCH,
      structureTranslation = mapOf("1" to "bonjour", "2" to "croissant", "3" to "deux"),
      version = 1
    )
    val settings = TranslationProjectSettings()
    settings.setTranslation(translationProperties)
    settings.checkState("""
      <TranslationProjectState>
        <option name="currentTranslationLanguage" value="French" />
        <option name="structureTranslation">
          <map>
            <entry key="French">
              <value>
                <map>
                  <entry key="1" value="bonjour" />
                  <entry key="2" value="croissant" />
                  <entry key="3" value="deux" />
                </map>
              </value>
            </entry>
          </map>
        </option>
        <option name="translationVersions">
          <map>
            <entry key="French" value="1" />
          </map>
        </option>
      </TranslationProjectState>
    """)
  }

  @Test(expected = ComparisonFailure::class)
  fun `test settings serialization when null language being set`() {
    val settings = TranslationProjectSettings()
    settings.loadStateAndCheck("""
      <TranslationProjectState>
        <option name="currentTranslationLanguage" />
        <option name="structureTranslation">
          <map>
            <entry key="Russian">
              <value>
                <map>
                  <entry key="1" value="привет" />
                  <entry key="2" value="круасан" />
                  <entry key="3" value="два" />
                </map>
              </value>
            </entry>
          </map>
        </option>
        <option name="translationVersions">
          <map>
            <entry key="Russian" value="1" />
          </map>
        </option>
      </TranslationProjectState>
    """)
  }
}