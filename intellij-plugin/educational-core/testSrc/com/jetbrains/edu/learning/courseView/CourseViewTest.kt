// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.edu.learning.courseView

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.Messages
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.edu.learning.EduTestDialog
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.actions.RevertTaskAction
import com.jetbrains.edu.learning.projectView.CourseViewPane
import com.jetbrains.edu.learning.projectView.FrameworkLessonNode
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.ui.getUICheckLabel
import com.jetbrains.edu.learning.withEduTestDialog
import org.junit.Test
import javax.swing.tree.DefaultMutableTreeNode

class CourseViewTest : CourseViewTestBase() {

  @Test
  fun testCoursePane() {
    createStudyCourse()
    configureByTaskFile(1, 1, "taskFile1.txt")
    val pane = createPane()
    PlatformTestUtil.waitForPromise(TreeUtil.promiseExpand(pane.tree, 3))

    val structure = """
                    -Project
                     -CourseNode Edu test course  0/4
                      -LessonNode lesson1
                       +TaskNode task1
                       +TaskNode task2
                       +TaskNode task3
                       +TaskNode task4
                    """.trimIndent()
    PlatformTestUtil.assertTreeEqual(pane.tree, structure)
  }

  @Test
  fun `test framework lesson navigate`() {
    courseWithFiles("Edu test course") {
      lesson {
        eduTask {
          taskFile("taskFile.txt")
        }
      }
      frameworkLesson {
        eduTask {
          taskFile("taskFile1.txt", "a = <p>TODO()</p>") {
            placeholder(0, "2")
          }
        }
        eduTask {
          taskFile("taskFile1.txt", "a = <p>TODO()</p>") {
            placeholder(0, "2")
          }
          taskFile("taskFile2.txt")
        }
      }
    }
    val pane = createPane()
    val model = pane.tree.model

    val structure = """
                    -Project
                     +CourseNode Edu test course  0/2
                    """.trimIndent()
    PlatformTestUtil.assertTreeEqual(pane.tree, structure)

    PlatformTestUtil.waitForPromise(TreeUtil.promiseExpand(pane.tree, 3))
    assertEmpty(FileEditorManager.getInstance(project).openFiles)

    val root = model.root as DefaultMutableTreeNode
    val courseNode = model.getChild(root, 0) as DefaultMutableTreeNode
    val frameworkLessonNode = model.getChild(courseNode, 1) as DefaultMutableTreeNode
    val frameworkLessonNodeObject = frameworkLessonNode.userObject as FrameworkLessonNode
    assertFalse(frameworkLessonNodeObject.expandOnDoubleClick())
    assertTrue(frameworkLessonNodeObject.canNavigate())
    frameworkLessonNodeObject.navigate(true)
    assertEquals(1, FileEditorManager.getInstance(project).openFiles.size)
    assertEquals("taskFile1.txt", FileEditorManager.getInstance(project).openFiles[0].name)
  }

  @Test
  fun testCourseProgress() {
    createStudyCourse()
    configureByTaskFile(1, 1, "taskFile1.txt")
    val pane = createPane()
    assertNotNull(pane.getProgressBar())
  }

  @Test
  fun testCheckTask() {
    createStudyCourse()
    configureByTaskFile(1, 1, "taskFile1.txt")

    val fileName = "lesson1/task1/taskFile1.txt"
    val taskFile = myFixture.findFileInTempDir(fileName)
    val task = findTask(0, 1)
    testAction(CheckAction(task.getUICheckLabel()), dataContext(taskFile))

    val structure = """
                    -Project
                     -CourseNode Edu test course  1/4
                      -LessonNode lesson1
                       -TaskNode task1
                        taskFile1.txt
                       -TaskNode task2
                        taskFile2.txt
                       -TaskNode task3
                        taskFile3.txt
                       -TaskNode task4
                        taskFile4.txt
                    """.trimIndent()
    assertCourseView(structure)

    withEduTestDialog(EduTestDialog(Messages.OK)) {
      testAction(RevertTaskAction.ACTION_ID, dataContext(taskFile))
    }
  }

  @Test
  fun `test hidden lesson`() {
    PropertiesComponent.getInstance().setValue(CourseViewPane.HIDE_SOLVED_LESSONS, true)
    try {
      courseWithFiles("Edu test course") {
        lesson {
          eduTask {
            taskFile("taskFile1.txt", "a = <p>TODO()</p>") {
              placeholder(0, "2")
            }
          }
        }
        lesson {
          eduTask {
            taskFile("taskFile2.txt", "a = <p>TODO()</p>") {
              placeholder(0, "2")
            }
          }
        }
      }
      configureByTaskFile(1, 1, "taskFile1.txt")

      val fileName = "lesson1/task1/taskFile1.txt"
      val taskFile = myFixture.findFileInTempDir(fileName)
      val task = findTask(0, 0)
      testAction(CheckAction(task.getUICheckLabel()), dataContext(taskFile))

      val structure = """
                      -Project
                       -CourseNode Edu test course  1/2
                        -LessonNode lesson2
                         -TaskNode task1
                          taskFile2.txt
                      """.trimIndent()
      assertCourseView(structure)

      withEduTestDialog(EduTestDialog(Messages.OK)) {
        testAction(RevertTaskAction.ACTION_ID, dataContext(taskFile))
      }
    } finally {
      PropertiesComponent.getInstance().setValue(CourseViewPane.HIDE_SOLVED_LESSONS, false)
    }
  }

  @Test
  fun `test course with custom content path`() {
    createStudyCourse("some/path")
    configureByTaskFile(1, 1, "taskFile1.txt")
    val pane = createPane()
    PlatformTestUtil.waitForPromise(TreeUtil.promiseExpand(pane.tree, 5))

    val structure = """
                    -Project
                     -CourseNode Edu test course  0/4
                      -IntermediateDirectoryNode some
                       -IntermediateDirectoryNode path
                        -LessonNode lesson1
                         +TaskNode task1
                         +TaskNode task2
                         +TaskNode task3
                         +TaskNode task4
                         """.trimIndent()
    PlatformTestUtil.assertTreeEqual(pane.tree, structure)
  }

  private fun createStudyCourse(customPath: String = "") {
    courseWithFiles("Edu test course", customPath = customPath) {
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

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/projectView"
  }
}
