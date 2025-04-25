package com.jetbrains.edu.coursecreator.courseView

import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseView.CourseViewTestBase
import org.junit.Test

class CCCourseViewTest : CourseViewTestBase() {

  @Test
  fun `test lesson with custom name`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
    }

    val lesson = course.lessons.first()
    lesson.customPresentableName = "custom name"

    doTest("-Project\n" +
                   " -CCCourseNode Test Course (Course Creation)\n" +
                   "  CCLessonNode custom name (lesson1)\n")
  }

  @Test
  fun `test section with custom name`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section()
    }

    val section = course.sections.first()
    section.customPresentableName = "custom name"

    doTest("-Project\n" +
                    " -CCCourseNode Test Course (Course Creation)\n" +
                    "  CCSectionNode custom name (section1)\n")
  }

  @Test
  fun `test task with custom name`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask()
        eduTask()
      }
    }

    val task = course.lessons.first().taskList.first()
    task.customPresentableName = "custom name"

    doTest("""
      -Project
       -CCCourseNode Test Course (Course Creation)
        -CCLessonNode lesson1
         -CCTaskNode custom name (task1)
          CCStudentInvisibleFileNode task.md
         -CCTaskNode task2
          CCStudentInvisibleFileNode task.md
    """.trimIndent())
  }

  @Test
  fun `test course view with custom content path`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, customPath = "some/path") {
      lesson {
        eduTask()
        eduTask()
      }
    }
    doTest("""
      -Project
       -CCCourseNode Test Course (Course Creation)
        -CCIntermediateDirectoryNode some
         -CCIntermediateDirectoryNode path
          -CCLessonNode lesson1
           -CCTaskNode task1
            CCStudentInvisibleFileNode task.md
           -CCTaskNode task2
            CCStudentInvisibleFileNode task.md
    """.trimIndent())
  }

  private fun doTest(structure: String) {
    val pane = createPane()
    PlatformTestUtil.waitForPromise(TreeUtil.promiseExpandAll(pane.tree))
    PlatformTestUtil.assertTreeEqual(pane.tree, structure)
  }
}