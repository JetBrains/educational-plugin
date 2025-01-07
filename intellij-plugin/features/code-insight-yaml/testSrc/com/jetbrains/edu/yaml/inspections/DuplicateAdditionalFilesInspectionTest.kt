package com.jetbrains.edu.yaml.inspections

import com.jetbrains.edu.learning.courseFormat.CourseMode
import org.junit.Test

class DuplicateAdditionalFilesInspectionTest : YamlInspectionsTestBase(DuplicateAdditionalFilesInspection::class) {

  @Test
  fun `warn on duplicate files`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {}

    val courseInfoYaml = """
      |title: Test Course
      |summary: sum
      |programming_language: Plain text
      |additional_files:
      |  - name: file1.txt
      |  - name: <error descr="Duplicate additional file">file2.txt</error>
      |  - name: <error descr="Duplicate additional file">file3.txt</error>
      |  - name: <error descr="Duplicate additional file">file2.txt</error>
      |  - name: <error descr="Duplicate additional file">file3.txt</error>
      |  - name: <error descr="Duplicate additional file">file3.txt</error>
      |  - name: file0.txt
    """.trimMargin("|")

    testHighlighting(course, courseInfoYaml)
  }
}
