package com.jetbrains.edu.yaml.inspections

import com.jetbrains.edu.learning.courseFormat.CourseMode
import org.junit.Test

class AdditionalFileWithoutNameInspectionTest : YamlInspectionsTestBase(AdditionalFileWithoutNameInspection::class) {

  @Test
  fun `test additional file is written without the name property`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
    }

    testHighlighting(
      course, """
      |title: Test Course
      |summary: sum
      |programming_language: Plain text
      |content:
      |- lesson1
      |additional_files:
      | - <error descr="Additional file must have the \"name\" property">a.txt</error>
      | - name: b.txt
      | - name: c.png
      |   is_binary: true
      | - <error descr="Additional file must have the \"name\" property">d.txt</error>
    """.trimMargin("|")
    )
  }
}
