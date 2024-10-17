package com.jetbrains.edu.yaml

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EnhancementsInfo
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.mapper
import com.jetbrains.edu.learning.yaml.YamlTestCase
import com.jetbrains.educational.translation.enum.Language
import org.junit.Test

class YamlSerializationTest: YamlTestCase() {

  @Test
  fun `test translation language`() {
    val course = course {
      lesson("lesson1") {
        eduTask()
      }
    }
    course.translatedToLanguageCode = "fr"
    doTest(course, """
      |title: Test Course
      |language: English
      |translated_to_language: French
      |summary: Test Course Description
      |programming_language: Plain text
      |content:
      |- lesson1
      |mode: Study
      |""".trimMargin())
  }

  @Test
  fun `test course enhancements info`() {
    val course = course {
      lesson("lesson1") {
        eduTask()
      }
    }
    course.translatedToLanguageCode = "fr"
    course.enhancements = mapOf(
      Language.FRENCH to EnhancementsInfo(1),
      Language.SIMPLIFIED_CHINESE to EnhancementsInfo(2)
    )
    doTest(course, """
      |title: Test Course
      |language: English
      |translated_to_language: French
      |enhancements:
      |  fr:
      |    translation_version: 1
      |  zh-CN:
      |    translation_version: 2
      |summary: Test Course Description
      |programming_language: Plain text
      |content:
      |- lesson1
      |mode: Study
      |""".trimMargin())
  }

  @Test
  fun `test empty course enhancements info`() {
    val course = course {
      lesson("lesson1") {
        eduTask()
      }
    }
    doTest(course, """
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |content:
      |- lesson1
      |mode: Study
      |""".trimMargin())
  }

  private fun doTest(item: StudyItem, expected: String) {
    val actual = item.course.mapper().writeValueAsString(item)
    assertEquals(expected, actual)
  }
}