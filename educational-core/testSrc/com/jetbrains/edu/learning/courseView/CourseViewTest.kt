// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.edu.learning.courseView

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.actions.RevertTaskAction
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.projectView.CourseViewPane
import junit.framework.TestCase
import org.junit.Assert

class CourseViewTest : CourseViewTestBase() {

  fun testCoursePane() {
    createStudyCourse()
    configureByTaskFile(1, 1, "taskFile1.txt")
    val pane = createPane()

    val structure = "-Project\n" +
                    " -CourseNode Edu test course  0/4\n" +
                    "  -LessonNode lesson1\n" +
                    "   +TaskNode task1\n" +
                    "   +TaskNode task2\n" +
                    "   +TaskNode task3\n" +
                    "   +TaskNode task4\n"
    PlatformTestUtil.assertTreeEqual(pane.tree, structure)
  }

  fun testProjectOpened() {
    createStudyCourse()
    val projectView = ProjectView.getInstance(project)
    projectView.refresh()
    projectView.changeView(CourseViewPane.ID)
    val pane = projectView.currentProjectViewPane
    waitWhileBusy(pane.tree)
    EduUtils.openFirstTask(StudyTaskManager.getInstance(project).course!!, project)
    PlatformTestUtil.waitForAlarm(600)
    waitWhileBusy(pane.tree)
    val structure = "-Project\n" +
                          " -CourseNode Edu test course  0/4\n" +
                          "  -LessonNode lesson1\n" +
                          "   -TaskNode task1\n" +
                          "    taskFile1.txt\n" +
                          "   +TaskNode task2\n" +
                          "   +TaskNode task3\n" +
                          "   +TaskNode task4\n"
    PlatformTestUtil.assertTreeEqual(pane.tree, structure)
  }

  fun testExpandAfterNavigation() {
    createStudyCourse()
    configureByTaskFile(1, 1, "taskFile1.txt")
    val projectView = ProjectView.getInstance(project)
    projectView.changeView(CourseViewPane.ID)
    navigateToNextTask()

    val pane = projectView.currentProjectViewPane
    val structure = "-Project\n" +
                    " -CourseNode Edu test course  0/4\n" +
                    "  -LessonNode lesson1\n" +
                    "   +TaskNode task1\n" +
                    "   -TaskNode task2\n" +
                    "    taskFile2.txt\n" +
                    "   +TaskNode task3\n" +
                    "   +TaskNode task4\n"
    PlatformTestUtil.assertTreeEqual(pane.tree, structure)
  }

  fun testCourseProgress() {
    createStudyCourse()
    configureByTaskFile(1, 1, "taskFile1.txt")
    val pane = createPane()
    TestCase.assertNotNull(pane.getProgressBar())
  }

  fun testSwitchingPane() {
    createStudyCourse()
    val projectView = ProjectView.getInstance(project)
    projectView.changeView(CourseViewPane.ID)
    TestCase.assertEquals(CourseViewPane.ID, projectView.currentViewId)
  }

  fun testCheckTask() {
    createStudyCourse()
    configureByTaskFile(1, 1, "taskFile1.txt")
    val projectView = ProjectView.getInstance(project)
    projectView.changeView(CourseViewPane.ID)

    val fileName = "lesson1/task1/taskFile1.txt"
    val taskFile = myFixture.findFileInTempDir(fileName)
    val action = CheckAction()
    launchAction(taskFile, action)

    val structure = "-Project\n" +
                    " -CourseNode Edu test course  1/4\n" +
                    "  -LessonNode lesson1\n" +
                    "   -TaskNode task1\n" +
                    "    taskFile1.txt\n" +
                    "   -TaskNode task2\n" +
                    "    taskFile2.txt\n" +
                    "   -TaskNode task3\n" +
                    "    taskFile3.txt\n" +
                    "   -TaskNode task4\n" +
                    "    taskFile4.txt"
    assertCourseView(structure)

    val refreshTaskFileAction = RevertTaskAction()
    withTestDialog(EduTestDialog(Messages.OK)) {
      launchAction(taskFile, refreshTaskFileAction)
    }
  }

  private fun createStudyCourse() {
    courseWithFiles("Edu test course") {
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
    }
  }

  private fun launchAction(taskFile: VirtualFile, action: AnAction) {
    val presentation = testAction(dataContext(taskFile), action)
    Assert.assertTrue(presentation.isEnabledAndVisible)
  }

  private fun navigateToNextTask() {
    val eduEditor = EduUtils.getSelectedEduEditor(project)
    val eduState = EduState(eduEditor)
    TestCase.assertTrue(eduState.isValid)
    val targetTask = NavigationUtils.nextTask(eduState.task)
    TestCase.assertNotNull(targetTask)
    NavigationUtils.navigateToTask(project, targetTask!!)
  }

  private fun createPane(): CourseViewPane {
    val pane = CourseViewPane(project)
    pane.createComponent()
    Disposer.register(project, pane)
    return pane
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/projectView"
  }
}
