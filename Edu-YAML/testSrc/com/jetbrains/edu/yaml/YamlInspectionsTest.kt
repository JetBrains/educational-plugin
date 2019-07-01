package com.jetbrains.edu.yaml

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.StudyItem
import org.jetbrains.yaml.schema.YamlJsonSchemaHighlightingInspection


class YamlInspectionsTest : YamlCodeInsightTest() {

  fun `test course with one wrong property`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {}
    }

    testHighlighting(getCourse(), """
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
  }

  fun `test section with one wrong property`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {}
      }
    }

    testHighlighting(getCourse().items[0], """
      |custom_name: "my awesome : section"
      |<warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property: prop</warning>
      |content:
      |- lesson1
    """.trimMargin("|"))
  }

  fun `test lesson with one wrong property`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {  }
      }
    }

    testHighlighting(getCourse().items[0], """
      |custom_name: "my awesome : lesson"
      |type: framework
      |<warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property: prop</warning>
      |content:
      |- task1
    """.trimMargin("|"))
  }

  private fun testHighlighting(item: StudyItem, configText: String) {
    openConfigFileWithText(item, configText)
    myFixture.enableInspections(YamlJsonSchemaHighlightingInspection::class.java)
    myFixture.checkHighlighting()
  }
}