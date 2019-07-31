package com.jetbrains.edu.learning.actions

import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class HyperskillTemplateBasedNavigationTest : NavigationTestBase() {

  fun `test type & next`() {
    val course = createHyperskillCourse()

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      task.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      task.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", """
              fun foobar() {}
            """)
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests2.kt", """
              fun tests2() {}
            """)
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test type & next prev`() {
    val course = createHyperskillCourse()

    withVirtualFileListener(course) {
      val task1 = course.findTask("lesson1", "task1")
      task1.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      task1.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun baz() {}\n")
      myFixture.testAction(PreviousTaskAction())
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", """
              fun bar() {}
              fun foo() {}
            """)
            file("Bar.kt", "fun bar() {}")
          }
          dir("test") {
            file("Tests1.kt", """
              fun tests1() {}
            """)
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test type & next prev next`() {
    val course = createHyperskillCourse()

    withVirtualFileListener(course) {
      val task1 = course.findTask("lesson1", "task1")
      task1.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      task1.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun baz() {}\n")
      task2.openTaskFileInEditor("src/Baz.kt")
      myFixture.type("fun qqq() {}\n")
      myFixture.testAction(PreviousTaskAction())

      task1.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun baz() {}\n")
      task1.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", """
              fun baz() {}
              fun foobar() {}
            """)
            file("Baz.kt", """
              fun qqq() {}
              fun baz() {}
            """)
          }
          dir("test") {
            file("Tests2.kt", """
              fun tests2() {}
            """)
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test create file & next`() {
    val course = createHyperskillCourse()

    withVirtualFileListener(course) {
      val task1 = course.findTask("lesson1", "task1")
      task1.createTaskFileAndOpenInEditor("NewFile.kt")
      myFixture.type("fun qwe() {}\n")
      task1.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", """
              fun foobar() {}
            """)
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests2.kt", """
              fun tests2() {}
            """)
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test create file & next prev`() {
    val course = createHyperskillCourse()

    withVirtualFileListener(course) {
      val task1 = course.findTask("lesson1", "task1")
      task1.createTaskFileAndOpenInEditor("src/NewFile.kt")
      myFixture.type("fun qwe() {}")
      task1.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("src/Baz.kt")
      myFixture.testAction(PreviousTaskAction())
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", "fun foo() {}")
            file("Bar.kt", "fun bar() {}")
            file("NewFile.kt", "fun qwe() {}")
          }
          dir("test") {
            file("Tests1.kt", """
              fun tests1() {}
            """)
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test remove file & next`() {
    val course = createHyperskillCourse()

    withVirtualFileListener(course) {
      val task1 = course.findTask("lesson1", "task1")
      task1.removeTaskFile("src/Task.kt")
      task1.openTaskFileInEditor("src/Bar.kt")
      task1.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", """
              fun foobar() {}
            """)
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests2.kt", """
              fun tests2() {}
            """)
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test remove file & next prev`() {
    val course = createHyperskillCourse()

    withVirtualFileListener(course) {
      val task1 = course.findTask("lesson1", "task1")
      task1.removeTaskFile("src/Task.kt")
      task1.openTaskFileInEditor("src/Bar.kt")
      task1.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("src/Baz.kt")
      myFixture.testAction(PreviousTaskAction())
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Bar.kt", "fun bar() {}")
          }
          dir("test") {
            file("Tests1.kt", """
              fun tests1() {}
            """)
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  private fun createHyperskillCourse(): Course {
    val course = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::HyperskillCourse
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1") {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Bar.kt", "fun bar() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
        eduTask("task2") {
          taskFile("src/Task.kt", "fun foobar() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests2.kt", "fun tests2() {}")
        }
      }
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject().apply { isTemplateBased = true }
    return course
  }
}
