package com.jetbrains.edu.learning.taskDescription

import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseView.CourseViewHeavyTestBase
import com.jetbrains.edu.learning.projectView.CourseViewPane
import com.jetbrains.edu.learning.taskDescription.ui.ToolWindowLinkHandler

class InCourseLinksCourseViewTest : CourseViewHeavyTestBase() {

  fun `test section link`() = doTest("course://section1", """
    -Project
     -CourseNode Test Course  0/2
      +LessonNode lesson1
      -[SectionNode section1]
       -LessonNode lesson2
        -TaskNode task2
         TaskFile2.txt  
  """)

  fun `test lesson link`() = doTest("course://lesson1", """
    -Project
     -CourseNode Test Course  0/2
      -[LessonNode lesson1]
       -TaskNode task1
        TaskFile1.txt
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

    ToolWindowLinkHandler.processInCourseLink(project, url)
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

    assertEquals(CourseViewPane.ID, projectView.currentViewId)
    PlatformTestUtil.assertTreeEqual(projectView.currentProjectViewPane.tree, expectedTree.trimIndent(), true)
  }
}
