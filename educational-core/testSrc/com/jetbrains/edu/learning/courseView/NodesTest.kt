// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.edu.learning.courseView

import com.intellij.ide.projectView.ProjectView
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.ProjectViewTestUtil
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.projectView.CourseViewPane

class NodesTest: EduTestCase() {
  override fun setUp() {
    super.setUp()
    ProjectViewTestUtil.setupImpl(project, true)
  }

  fun testSections() {
    courseWithFiles {
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

    assertCourseView("-Project\n" +
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
    )
  }

  fun testTaskFilesOrder() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("C.txt")
          taskFile("B.txt")
          taskFile("A.txt")
        }

        eduTask {
          taskFile("taskFile.txt")
        }
      }
    }

    assertCourseView("""
    |-Project
    | -CourseNode Test Course  0/2
    |  -LessonNode lesson1
    |   -TaskNode task1
    |    C.txt
    |    B.txt
    |    A.txt
    |   -TaskNode task2
    |    taskFile.txt
    """.trimMargin("|"))
  }

  private fun assertCourseView(structure: String) {
    val projectView = ProjectView.getInstance(project)
    projectView.refresh()
    projectView.changeView(CourseViewPane.ID)
    val pane = projectView.currentProjectViewPane
    val tree = pane.tree
    PlatformTestUtil.waitWhileBusy(tree)
    TreeUtil.expandAll(tree)
    PlatformTestUtil.waitWhileBusy(tree)
    PlatformTestUtil.assertTreeEqual(tree, structure)
  }
}
