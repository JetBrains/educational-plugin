package com.jetbrains.edu.yaml

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeCourse
import com.jetbrains.edu.learning.yaml.YamlMapper.basicMapper
import com.jetbrains.edu.learning.yaml.YamlTestCase
import com.jetbrains.educational.translation.enum.Language
import org.junit.Test

class YamlDeserializationTest : YamlTestCase() {
  private val objectMapper = basicMapper()

  @Test
  fun `test translation language`() {
    val yamlContent = """
      |title: Test Course
      |language: English
      |translated_to_language: Russian
      |summary: Test Course Description
      |programming_language: Plain text
      |content:
      |- lesson1
      |mode: Study
      |""".trimMargin()
    val course = deserializeNotNull(yamlContent)
    assertEquals("ru", course.translatedToLanguageCode)
  }

  @Test
  fun `test course enhancements info`() {
    val yamlContent = """
      |title: Test Course
      |language: English
      |translated_to_language: Russian
      |enhancements:
      |  ru:
      |    translation_version: 1
      |  es:
      |    translation_version: 2
      |summary: Test Course Description
      |programming_language: Plain text
      |content:
      |- lesson1
      |mode: Study
      |""".trimMargin()
    val course = deserializeNotNull(yamlContent)
    assertEquals(2, course.enhancements.size)
    val russianTranslation = course.enhancements[Language.RUSSIAN] ?: error("Enhancement in russian is not found")
    assertEquals(1, russianTranslation.translationVersion)
    val spanishTranslation = course.enhancements[Language.SPANISH] ?: error("Enhancement in spanish is not found")
    assertEquals(2, spanishTranslation.translationVersion)
  }

  @Test
  fun `test empty course enhancements info`() {
    val yamlContent = """
      |title: Test Course
      |language: English
      |translated_to_language: Russian
      |summary: Test Course Description
      |programming_language: Plain text
      |content:
      |- lesson1
      |mode: Study
      |""".trimMargin()
    val course = deserializeNotNull(yamlContent)
    assertEquals(0, course.enhancements.size)
  }

  private fun deserializeNotNull(yamlContent: String): Course = objectMapper.deserializeCourse(yamlContent)
}