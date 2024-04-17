package com.jetbrains.edu.java.courseView

import com.intellij.ide.projectView.impl.ProjectViewState
import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.learning.courseView.CourseViewTestBase
import org.junit.Test

class JNodesTests : CourseViewTestBase() {

  override fun setUp() {
    super.setUp()
    // Since 2020.1 this setting is disabled by default in tests
    ProjectViewState.getInstance(project).hideEmptyMiddlePackages = true
  }

  @Test
  fun `test nested packages`() {
    courseWithFiles(language = JavaLanguage.INSTANCE) {
      lesson {
        eduTask {
          dir("src") {
            taskFile("Task1.java")
            taskFile("foo/bar/Task2.java")
          }
          taskFile("Task3.java")
          taskFile("test/Tests.java")
        }

        eduTask {
          taskFile("src/Task1.java")
          taskFile("Task2.java")
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
      |    Task3.java
      |   -TaskNode task2
      |    -DirectoryNode src
      |     Task1.java
      |    Task2.java
    """.trimMargin())
  }
}
