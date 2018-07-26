// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.edu.learning.courseView

class NodesTest: CourseViewTestBase() {

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

    assertCourseView("""
    |-Project
    | -CourseNode Test Course  0/10
    |  -LessonNode lesson1
    |   -TaskNode task1
    |    taskFile1.txt
    |   -TaskNode task2
    |    taskFile2.txt
    |   -TaskNode task3
    |    taskFile3.txt
    |   -TaskNode task4
    |    taskFile4.txt
    |  -SectionNode section2
    |   -LessonNode lesson1
    |    -TaskNode task1
    |     taskFile1.txt
    |    -TaskNode task2
    |     taskFile1.txt
    |   -LessonNode lesson2
    |    -TaskNode task1
    |     taskFile1.txt
    |    -TaskNode task2
    |     taskFile2.txt
    |  -LessonNode lesson2
    |   -TaskNode task1
    |    taskFile1.txt
    |   -TaskNode task2
    |    taskFile2.txt
    """.trimMargin("|"))
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
}
