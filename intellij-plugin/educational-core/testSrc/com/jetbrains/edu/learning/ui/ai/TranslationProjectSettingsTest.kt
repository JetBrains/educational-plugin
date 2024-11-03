package com.jetbrains.edu.learning.ui.ai

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import com.jetbrains.edu.learning.ai.CourseStructureNames
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.ai.TranslationProperties
import com.jetbrains.educational.core.enum.TranslationLanguage
import com.jetbrains.educational.core.format.domain.*
import com.jetbrains.educational.translation.format.domain.TranslationVersion
import junit.framework.ComparisonFailure
import org.junit.Test

class TranslationProjectSettingsTest : EduSettingsServiceTestBase() {
  @Suppress("NonAsciiCharacters")
  @Test
  fun `test serialization`() {
    val settings = TranslationProjectSettings()
    settings.loadStateAndCheck("""
      <TranslationProjectState>
        <option name="currentTranslationLanguage" value="Russian" />
        <option name="structureTranslation">
          <map>
            <entry key="Russian" value="{&quot;taskNames&quot;:{&quot;1&quot;:&quot;привет&quot;},&quot;lessonNames&quot;:{&quot;1&quot;:&quot;круасан&quot;},&quot;sectionNames&quot;:{&quot;1&quot;:&quot;два&quot;}}" />
            <entry key="French" value="{&quot;taskNames&quot;:{&quot;1&quot;:&quot;bonjour&quot;},&quot;lessonNames&quot;:{&quot;1&quot;:&quot;croissant&quot;},&quot;sectionNames&quot;:{&quot;1&quot;:&quot;deux&quot;}}" />
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
    val courseTranslationStructure = CourseStructureNames(
      taskNames = mapOf(TaskEduId(1) to TaskName("bonjour")),
      lessonNames = mapOf(LessonEduId(1) to LessonName("croissant")),
      sectionNames = mapOf(SectionEduId(1) to SectionName("deux"))
    )
    val translationProperties = TranslationProperties(
      language = TranslationLanguage.FRENCH,
      structureTranslation = courseTranslationStructure,
      version = TranslationVersion(1)
    )
    val settings = TranslationProjectSettings()
    settings.setTranslation(translationProperties)
    settings.checkState("""
      <TranslationProjectState>
        <option name="currentTranslationLanguage" value="French" />
        <option name="structureTranslation">
          <map>
            <entry key="French" value="{&quot;taskNames&quot;:{&quot;1&quot;:&quot;bonjour&quot;},&quot;lessonNames&quot;:{&quot;1&quot;:&quot;croissant&quot;},&quot;sectionNames&quot;:{&quot;1&quot;:&quot;deux&quot;}}" />
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

  @Suppress("NonAsciiCharacters")
  @Test(expected = ComparisonFailure::class)
  fun `test settings serialization when null language being set`() {
    val settings = TranslationProjectSettings()
    settings.loadStateAndCheck("""
      <TranslationProjectState>
        <option name="currentTranslationLanguage" />
        <option name="structureTranslation">
          <map>
            <entry key="Russian" value="{&quot;taskNames&quot;:{&quot;1&quot;:&quot;привет&quot;},&quot;lessonNames&quot;:{&quot;1&quot;:&quot;круасан&quot;},&quot;sectionNames&quot;:{&quot;1&quot;:&quot;два&quot;}}" />
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