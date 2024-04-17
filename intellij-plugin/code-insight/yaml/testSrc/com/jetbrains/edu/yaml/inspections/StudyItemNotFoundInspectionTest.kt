package com.jetbrains.edu.yaml.inspections

import com.jetbrains.edu.coursecreator.actions.create.MockNewStudyItemUi
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import org.junit.Test

class StudyItemNotFoundInspectionTest : YamlInspectionsTestBase(StudyItemNotFoundInspection::class) {

  @Test
  fun `test create missing task`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
    }

    val lesson = course.getLesson("lesson1")!!
    val expectedItems = listOf("task1", "task2")
    doTest(lesson, "Create task", """
      |content:
      |- task1
      |- <error descr="Cannot find `task2` task">task2<caret></error>
    """.trimMargin("|"), """
      |content:
      |- task1
      |- task2
    """.trimMargin("|"), expectedItems)
  }

  @Test
  fun `test create missing lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section("section1") {
        lesson("lesson1") {}
      }
    }

    val section = course.getSection("section1")!!
    val expectedItems = listOf("lesson0", "lesson1")
    doTest(section, "Create lesson", """
      |content:
      |- <error descr="Cannot find `lesson0` lesson">lesson0<caret></error>
      |- lesson1
    """.trimMargin("|"), """
      |content:
      |- lesson0
      |- lesson1
    """.trimMargin("|"), expectedItems)
  }

  @Test
  fun `test create missing lesson in course`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {}
    }

    val expectedItems = listOf("lesson1", "lesson2")
    doTest(course, "Create lesson", """
      |title: Test Course
      |summary: sum
      |programming_language: Plain text
      |content:
      |- lesson1
      |- <error>lesson2<caret></error>
    """.trimMargin("|"), """
      |title: Test Course
      |summary: sum
      |programming_language: Plain text
      |content:
      |- lesson1
      |- lesson2
    """.trimMargin("|"), expectedItems)
  }

  @Test
  fun `test create missing section in course`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section("section1") {}
    }

    val expectedItems = listOf("section0", "section1")
    doTest(course, "Create section", """
      |title: Test Course
      |summary: sum
      |programming_language: Plain text
      |content:
      |- <error>section0<caret></error>
      |- section1
    """.trimMargin("|"), """
      |title: Test Course
      |summary: sum
      |programming_language: Plain text
      |content:
      |- section0
      |- section1
    """.trimMargin("|"), expectedItems)
  }

  @Test
  fun `test do not provide quick fix for invalid paths`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section("section1") {}
    }

    val section = course.getSection("section1")!!
    testQuickFixIsUnavailable(section, "Create lesson", """
      |content:
      |- <error descr="Cannot find `les\son1` lesson">les\son1<caret></error>
    """.trimMargin("|"))
  }

  private fun doTest(itemContainer: ItemContainer, quickFixName: String, before: String, after: String, expectedItems: List<String>) {
    withMockCreateStudyItemUi(MockNewStudyItemUi()) {
      testQuickFix(itemContainer, quickFixName, before, after)
    }
    val actualItems = itemContainer.items.map { it.name }
    assertEquals(expectedItems, actualItems)
    val itemContainerDir = itemContainer.getDir(project.courseDir)!!
    for (name in expectedItems) {
      assertNotNull("Failed to find `$name` directory", itemContainerDir.findFileByRelativePath(name))
    }
  }
}
