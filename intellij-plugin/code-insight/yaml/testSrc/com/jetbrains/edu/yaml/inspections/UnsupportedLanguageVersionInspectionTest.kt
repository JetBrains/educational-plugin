package com.jetbrains.edu.yaml.inspections

import com.jetbrains.edu.learning.courseFormat.CourseMode
import org.junit.Test

class UnsupportedLanguageVersionInspectionTest : YamlInspectionsTestBase(UnsupportedLanguageVersionInspection::class) {

  @Test
  fun `test unsupported version`() {
    val version = "8"
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
    }.apply { languageVersion = version }

    testHighlighting(
      course, """
      |title: Test Course
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: <error descr="Unsupported language 'Plain text', version: '8'">$version</error>
      |content:
      |- lesson1
    """.trimMargin("|")
    )
  }
  @Test
  fun `test supported version`() {
    val version = "1.42"
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
    }.apply { languageVersion = version }

    testHighlighting(
      course, """
      |title: Test Course
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: $version
      |content:
      |- lesson1
    """.trimMargin("|")
    )
  }
}
