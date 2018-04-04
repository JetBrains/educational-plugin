// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.edu.learning.courseView

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.ProjectViewTestUtil
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.projectView.CourseViewPane
import java.io.IOException

class SectionsTest : EduTestCase() {

  private var myCourse: Course? = null

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myCourse?.language = PlainTextLanguage.INSTANCE.id
    ProjectViewTestUtil.setupImpl(project, true)
  }

  fun testSections() {
    val section = Section()
    section.name = "Test section"
    val projectView = ProjectView.getInstance(project)
    projectView.refresh()
    projectView.changeView(CourseViewPane.ID)
    val pane = projectView.currentProjectViewPane
    val tree = pane.tree
    val structure = "-Project\n" +
                    " -CourseNode Test Course  0/10\n" +
                    "  -LessonNode lesson1\n" +
                    "   -TaskNode task1\n" +
                    "    taskFile1.txt\n" +
                    "   -TaskNode task2\n" +
                    "    taskFile2.txt\n" +
                    "   -TaskNode task3\n" +
                    "    taskFile3.txt\n" +
                    "   -TaskNode task4\n" +
                    "    taskFile4.txt\n" +
                    "  -SectionNode section2\n" +
                    "   -LessonNode lesson1\n" +
                    "    -TaskNode task1\n" +
                    "     taskFile1.txt\n" +
                    "    -TaskNode task2\n" +
                    "     taskFile1.txt\n" +
                    "   -LessonNode lesson2\n" +
                    "    -TaskNode task1\n" +
                    "     taskFile1.txt\n" +
                    "    -TaskNode task2\n" +
                    "     taskFile2.txt\n" +
                    "  -LessonNode lesson2\n" +
                    "   -TaskNode task1\n" +
                    "    taskFile1.txt\n" +
                    "   -TaskNode task2\n" +
                    "    taskFile2.txt\n"
    PlatformTestUtil.waitWhileBusy(tree)
    TreeUtil.expandAll(tree)
    PlatformTestUtil.waitWhileBusy(tree)
    PlatformTestUtil.assertTreeEqual(tree, structure)
  }

  @Throws(IOException::class)
  override fun createCourse() {
    myCourse = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
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
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
          eduTask {
            taskFile("taskFile2.txt")
          }
        }
      }
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
        eduTask {
          taskFile("taskFile2.txt")
        }
      }
    }
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/sections"
  }
}
