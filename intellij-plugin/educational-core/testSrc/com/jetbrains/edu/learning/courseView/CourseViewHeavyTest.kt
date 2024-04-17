package com.jetbrains.edu.learning.courseView

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.BuildNumber
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.projectView.CourseViewPane
import org.junit.Test
import javax.swing.JTree

class CourseViewHeavyTest : CourseViewHeavyTestBase() {
  override val defaultSettings: EmptyProjectSettings = EmptyProjectSettings

  @Test
  fun testProjectOpened() {
    val projectView = createCourseAndChangeView()

    val tree = projectView.currentProjectViewPane.tree

    PlatformTestUtil.waitForAlarm(600)
    waitWhileBusy(tree)

    PlatformTestUtil.assertTreeEqual(tree, """
      -Project
       -CourseNode Edu test course  0/4
        -LessonNode lesson1
         -TaskNode task1
          taskFile1.txt
         +TaskNode task2
         +TaskNode task3
         +TaskNode task4
    """.trimIndent())
  }

  @Test
  fun testExpandAfterNavigation() {
    // https://youtrack.jetbrains.com/issue/EDU-5367
    if (ApplicationInfo.getInstance().build >= BUILD_223) {
      return
    }

    val projectView = createCourseAndChangeView()

    val tree = projectView.currentProjectViewPane.tree

    navigateToNextTask()
    waitWhileBusy(tree)

    PlatformTestUtil.assertTreeEqual(tree, """
      -Project
       -CourseNode Edu test course  0/4
        -LessonNode lesson1
         +TaskNode task1
         -TaskNode task2
          taskFile2.txt
         +TaskNode task3
         +TaskNode task4
    """.trimIndent())
  }

  @Test
  fun testSwitchingPane() {
    val projectView = createCourseAndChangeView()
    assertEquals(CourseViewPane.ID, projectView.currentViewId)
  }

  private fun navigateToNextTask() {
    val sourceTask = FileEditorManager.getInstance(project).openFiles[0].getContainingTask(project) ?: error("No opened task")
    val targetTask = NavigationUtils.nextTask(sourceTask) ?: error("Can't navigate to task")
    NavigationUtils.navigateToTask(project, targetTask)
  }

  private fun waitWhileBusy(tree: JTree) {
    PlatformTestUtil.waitWhileBusy(tree)
  }

  private fun createCourseAndChangeView(): ProjectView = createCourseAndChangeView(course("Edu test course") {
    lesson {
      eduTask {
        taskFile("taskFile1.txt", "a = <p>TODO()</p>") {
          placeholder(0, "2")
        }
      }
      eduTask {
        taskFile("taskFile2.txt")
      }
      eduTask {
        taskFile("taskFile3.txt")
      }
      eduTask {
        taskFile("taskFile4.txt")
      }
    }
  })

  companion object {
    private val BUILD_223: BuildNumber = BuildNumber.fromString("223")!!
  }
}
