package com.jetbrains.edu.learning.courseView

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.ProjectViewTestUtil
import com.jetbrains.edu.learning.CourseGenerationTestBase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.projectView.CourseViewPane
import javax.swing.JTree

class CourseViewHeavyTest : CourseGenerationTestBase<Unit>() {
  override val defaultSettings: Unit = Unit

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

  fun testExpandAfterNavigation() {
    val projectView = createCourseAndChangeView()

    navigateToNextTask()

    PlatformTestUtil.assertTreeEqual(projectView.currentProjectViewPane.tree, """
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

  fun testSwitchingPane() {
    val projectView = createCourseAndChangeView()
    assertEquals(CourseViewPane.ID, projectView.currentViewId)
  }

  private fun navigateToNextTask() {
    val sourceTask = EduUtils.getTaskForFile(project, FileEditorManager.getInstance(project).openFiles[0]) ?: error("No opened task")
    val targetTask = NavigationUtils.nextTask(sourceTask) ?: error("Can't navigate to task")
    NavigationUtils.navigateToTask(project, targetTask)
  }

  private fun waitWhileBusy(tree: JTree) {
    PlatformTestUtil.waitWhileBusy(tree)
  }

  private fun createCourseAndChangeView(): ProjectView {
    createCourseStructure(course("Edu test course") {
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

    // can't do it in setUp because project is not opened at that point
    ProjectViewTestUtil.setupImpl(project, true)

    return ProjectView.getInstance(project).apply {
      refresh()
      changeView(CourseViewPane.ID)
      EduUtils.openFirstTask(StudyTaskManager.getInstance(project).course!!, project)
    }
  }
}