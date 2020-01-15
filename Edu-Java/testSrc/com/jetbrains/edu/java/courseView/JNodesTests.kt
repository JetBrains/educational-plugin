package com.jetbrains.edu.java.courseView

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.courseView.CourseViewTestBase

class JNodesTests : CourseViewTestBase() {

  fun `test nested packages`() {
    courseWithFiles(language = JavaLanguage.INSTANCE, settings = JdkProjectSettings.emptySettings()) {
      lesson {
        eduTask {
          dir("src") {
            taskFile("Task1.java")
            taskFile("foo/bar/Task2.java")
          }
          taskFile("test/Tests.java")
        }

        eduTask {
          taskFile("src/Task1.java")
          taskFile("test/Tests.java")
        }
      }
    }

    assertCourseView("""
      |-Project
      | -CourseNode Test Course  0/2
      |  -LessonNode lesson1
      |   -TaskNode task1
      |    -DirectoryNode src
      |     -DirectoryNode foo.bar
      |      Task2.java
      |     Task1.java
      |    -DirectoryNode test
      |     Tests.java
      |   -TaskNode task2
      |    -DirectoryNode src
      |     Task1.java
      |    -DirectoryNode test
      |     Tests.java
    """.trimMargin())
  }
}
