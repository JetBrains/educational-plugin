package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseView.CourseViewHeavyTestBase
import com.jetbrains.edu.learning.projectView.CourseViewPane
import org.junit.Test

class InCourseLinksCourseViewTest : CourseViewHeavyTestBase() {

  @Test
  fun `test section link`() = doTest("course://section1", """
    -Project
     -CourseNode Test Course  0/2
      +LessonNode lesson1
      -[SectionNode section1]
       +LessonNode lesson2
  """)

  @Test
  fun `test lesson link`() = doTest("course://lesson1", """
    -Project
     -CourseNode Test Course  0/2
      -[LessonNode lesson1]
       +TaskNode task1
      +SectionNode section1     
  """)

  private fun doTest(url: String, expectedTree: String) {
    val course = course {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("TaskFile1.txt")
        }
      }
      section("section1") {
        lesson("lesson2") {
          eduTask("task2") {
            taskFile("TaskFile2.txt")
          }
        }
      }
    }

    val projectView = createCourseAndChangeView(course, openFirstTask = false)

    ToolWindowLinkHandler(project).process(url)

    var lastError: AssertionError? = null
    repeat(10) {
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
      val tree = projectView.currentProjectViewPane.tree
      PlatformTestUtil.waitWhileBusy(tree)

      assertEquals(CourseViewPane.ID, projectView.currentViewId)
      try {
        PlatformTestUtil.assertTreeEqual(tree, expectedTree.trimIndent(), true)
        return
      }
      catch (e: AssertionError) {
        lastError = e
      }

      Thread.sleep(50)
    }

    throw lastError!!
  }
}
