// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.edu.learning.courseView

import com.intellij.openapi.application.runWriteAction
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.gradle.JdkProjectSettings

class NodesTest: CourseViewTestBase() {

  fun testOutsideScrDir() {
    courseWithFiles(language = FakeGradleBasedLanguage, settings = JdkProjectSettings.emptySettings()) {
      lesson {
        eduTask {
          taskFile("src/file.txt")
          taskFile("test/file.txt")
        }

        eduTask {
          taskFile("src/file.txt")
          taskFile("test/file.txt")
        }
      }
    }

    assertCourseView("""
    |-Project
    | -CourseNode Test Course  0/2
    |  -LessonNode lesson1
    |   -TaskNode task1
    |    -DirectoryNode src
    |     file.txt
    |    -DirectoryNode test
    |     file.txt
    |   -TaskNode task2
    |    -DirectoryNode src
    |     file.txt
    |    -DirectoryNode test
    |     file.txt
    """.trimMargin("|"))
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

  fun `test invisible files in student mode`() {
    courseWithInvisibleItems(EduNames.STUDY)
    assertCourseView("""
      -Project
       -CourseNode Test Course  0/2
        -LessonNode lesson1
         -TaskNode task1
          -DirectoryNode folder1
           taskFile3.txt
          taskFile1.txt
         -TaskNode task2
          additionalFile1.txt
          -DirectoryNode folder
           additionalFile3.txt
    """.trimIndent())
  }

  fun `test invisible files in educator mode`() {
    courseWithInvisibleItems(CCUtils.COURSE_MODE)
    assertCourseView("""
      -Project
       -CCCourseNode Test Course (Course Creation)
        -CCLessonNode lesson1
         -CCTaskNode task1
          -CCNode folder1
           taskFile3.txt
           CCStudentInvisibleFileNode taskFile4.txt
          CCStudentInvisibleFileNode task.html
          taskFile1.txt
          CCStudentInvisibleFileNode taskFile2.txt
         -CCTaskNode task2
          additionalFile1.txt
          CCStudentInvisibleFileNode additionalFile2.txt
          -CCNode folder
           additionalFile3.txt
           CCStudentInvisibleFileNode additionalFile4.txt
          CCStudentInvisibleFileNode task.html
    """.trimIndent())
  }

  fun `test non course files`() {
    courseWithInvisibleItems(CCUtils.COURSE_MODE)
    runWriteAction {
      LightPlatformTestCase.getSourceRoot().createChildData(NodesTest::class.java, "non_course_file1.txt")
      findFile("lesson1/task1").createChildData(NodesTest::class.java, "non_course_file2.txt")
      findFile("lesson1/task2/folder").createChildData(NodesTest::class.java, "non_course_file3.txt")
    }

    assertCourseView("""
      -Project
       -CCCourseNode Test Course (Course Creation)
        -CCLessonNode lesson1
         -CCTaskNode task1
          -CCNode folder1
           taskFile3.txt
           CCStudentInvisibleFileNode taskFile4.txt
          CCStudentInvisibleFileNode non_course_file2.txt (excluded)
          CCStudentInvisibleFileNode task.html
          taskFile1.txt
          CCStudentInvisibleFileNode taskFile2.txt
         -CCTaskNode task2
          additionalFile1.txt
          CCStudentInvisibleFileNode additionalFile2.txt
          -CCNode folder
           additionalFile3.txt
           CCStudentInvisibleFileNode additionalFile4.txt
           CCStudentInvisibleFileNode non_course_file3.txt (excluded)
          CCStudentInvisibleFileNode task.html
        CCStudentInvisibleFileNode non_course_file1.txt
    """.trimIndent())
  }

  private fun courseWithInvisibleItems(courseMode: String) {
    courseWithFiles(courseMode = courseMode) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
          taskFile("taskFile2.txt", visible = false)
          dir("folder1") {
            taskFile("taskFile3.txt")
            taskFile("taskFile4.txt", visible = false)
          }
        }
        eduTask {
          taskFile("additionalFile1.txt")
          taskFile("additionalFile2.txt", visible = false)
          dir("folder") {
            taskFile("additionalFile3.txt")
            taskFile("additionalFile4.txt", visible = false)
          }
        }
      }
    }
  }
}
