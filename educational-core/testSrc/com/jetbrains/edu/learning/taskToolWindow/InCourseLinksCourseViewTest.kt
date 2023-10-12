package com.jetbrains.edu.learning.taskToolWindow

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseView.CourseViewHeavyTestBase
import com.jetbrains.edu.learning.projectView.CourseViewPane
import com.jetbrains.edu.learning.taskToolWindow.ui.ToolWindowLinkHandler

class InCourseLinksCourseViewTest : CourseViewHeavyTestBase() {

  override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
    if (ApplicationInfo.getInstance().build < BuildNumber.fromString("233")!!) {
      super.runTestRunnable(testRunnable)
    }
  }

  fun `test section link`() = doTest("course://section1", """
    -Project
     -CourseNode Test Course  0/2
      +LessonNode lesson1
      -[SectionNode section1]
       +LessonNode lesson2
  """)

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
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

    val tree = projectView.currentProjectViewPane.tree
    PlatformTestUtil.waitWhileBusy(tree)

    assertEquals(CourseViewPane.ID, projectView.currentViewId)
    PlatformTestUtil.assertTreeEqual(tree, expectedTree.trimIndent(), true)
  }
}
