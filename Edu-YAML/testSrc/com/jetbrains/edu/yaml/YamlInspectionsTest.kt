package com.jetbrains.edu.yaml

import com.jetbrains.edu.coursecreator.CCUtils
import org.jetbrains.yaml.schema.YamlJsonSchemaHighlightingInspection


class YamlInspectionsTest : YamlCodeInsightTest() {

  fun `test course no highlighting`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {}
    }

    openConfigFileWithText(getCourse(), """
      |title: Test Course
      |type: coursera
      |language: Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |environment: Android
      |content:
      |- lesson1
      |
    """.trimMargin("|"))

    myFixture.enableInspections(YamlJsonSchemaHighlightingInspection::class.java)
    myFixture.checkHighlighting()
  }
}