package com.jetbrains.edu.learning.ui.ai.terms

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import com.jetbrains.edu.learning.ai.terms.TermsProjectSettings
import com.jetbrains.edu.learning.ai.terms.TermsProperties
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.terms.format.Term
import com.jetbrains.educational.terms.format.domain.TermsVersion
import org.junit.Test

class TermsProjectSettingTest : EduSettingsServiceTestBase() {
  private val termsStorage
    get() = TermsProjectSettings.getInstance(project)

  @Test
  fun `test empty storage serialization`() {
    termsStorage.checkState("""
      <TermsProjectState />
    """.trimIndent())
  }

  @Test
  fun `test serialization`() {
    with(termsStorage) {
      val termsProperties1 = TermsProperties(
        languageCode = TranslationLanguage.ENGLISH.code,
        terms = mapOf(TASK_ID to listOf(Term("A", "B"), Term("B", "C"))),
        version = TermsVersion(1)
      )
      setTerms(termsProperties1)
      checkState("""
      <TermsProjectState>
        <currentTermsLanguage>en</currentTermsLanguage>
        <terms>
          <map>
            <entry key="en">
              <value>
                <map>
                  <entry key="100">
                    <value>
                      <list>
                        <Term value="A" definition="B" />
                        <Term value="B" definition="C" />
                      </list>
                    </value>
                  </entry>
                </map>
              </value>
            </entry>
          </map>
        </terms>
        <termsVersions>
          <map>
            <entry key="en" value="1" />
          </map>
        </termsVersions>
      </TermsProjectState>
      """.trimIndent())

      cleanUpState()
      checkState("""
         <TermsProjectState />
      """.trimIndent()
      )
    }
  }

  @Test
  fun `test adding term translation for another language`() {
    with(termsStorage) {
      val termsProperties1 = TermsProperties(
        languageCode = TranslationLanguage.ENGLISH.code,
        terms = mapOf(TASK_ID to listOf(Term("A", "B"), Term("B", "C"))),
        version = TermsVersion(1)
      )
      setTerms(termsProperties1)

      val termsProperties2 = TermsProperties(
        languageCode = TranslationLanguage.RUSSIAN.code,
        terms = mapOf(TASK_ID to listOf(Term("X", "Y"), Term("Y", "Z"))),
        version = TermsVersion(2)
      )
      setTerms(termsProperties2)
      checkState("""
      <TermsProjectState>
        <currentTermsLanguage>ru</currentTermsLanguage>
        <terms>
          <map>
            <entry key="en">
              <value>
                <map>
                  <entry key="100">
                    <value>
                      <list>
                        <Term value="A" definition="B" />
                        <Term value="B" definition="C" />
                      </list>
                    </value>
                  </entry>
                </map>
              </value>
            </entry>
            <entry key="ru">
              <value>
                <map>
                  <entry key="100">
                    <value>
                      <list>
                        <Term value="X" definition="Y" />
                        <Term value="Y" definition="Z" />
                      </list>
                    </value>
                  </entry>
                </map>
              </value>
            </entry>
          </map>
        </terms>
        <termsVersions>
          <map>
            <entry key="en" value="1" />
            <entry key="ru" value="2" />
          </map>
        </termsVersions>
      </TermsProjectState>
      """.trimIndent())

      cleanUpState()
      checkState("""
         <TermsProjectState />
      """.trimIndent()
      )
    }
  }

  companion object {
    private const val TASK_ID = 100
  }
}