package com.jetbrains.edu.yaml

import com.jetbrains.edu.coursecreator.CCUtils
import org.jetbrains.yaml.schema.YamlJsonSchemaHighlightingInspection


class YamlInspectionsTest : YamlCodeInsightTest() {

  fun `test course with one wrong property`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {}
    }

    openConfigFileWithText(getCourse(), """
      |title: Test Course
      |type: coursera
      |language: Russian
      |summary: sum
      |<warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property: prop</warning>
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

  fun `test section with one wrong property`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {}
      }
    }

    openConfigFileWithText(getCourse().items[0], """
      |custom_name: "my awesome : section"
      |<warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property: prop</warning>
      |content:
      |- lesson1
    """.trimMargin("|"))

    myFixture.enableInspections(YamlJsonSchemaHighlightingInspection::class.java)
    myFixture.checkHighlighting()
  }
}