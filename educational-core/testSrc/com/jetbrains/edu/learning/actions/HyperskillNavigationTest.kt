package com.jetbrains.edu.learning.actions

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.EduTestDialog
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.withTestDialog
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat

class HyperskillNavigationTest : NavigationTestBase() {

  fun `test propagate user changes to next task (next)`() {
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
              fun bar() {}
              fun foo() {}
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

  fun `test do not propagate user changes to prev task (next, prev)`() {
    val course = createHyperskillCourse()

    withVirtualFileListener(course) {
      val task1 = course.findTask("lesson1", "task1")
      task1.openTaskFileInEditor("src/Task.kt")
      task1.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      myFixture.testAction(PreviousTaskAction())
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", """
              fun foo() {}
            """)
            file("Baz.kt", "fun baz() {}")
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

  fun `test ask user when changes conflicts (next, prev, next) 1`() {
    val course = createHyperskillCourse()

    withVirtualFileListener(course) {
      val task1 = course.findTask("lesson1", "task1")
      task1.openTaskFileInEditor("src/Task.kt")
      task1.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      myFixture.testAction(PreviousTaskAction())


      task1.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun baz() {}\n")

      withTestDialog(EduTestDialog(Messages.NO)) {
        myFixture.testAction(NextTaskAction())
      }.checkWasShown()
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", """
              fun bar() {}
              fun foo() {}
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

  fun `test ask user when changes conflicts (next, prev, next) 2`() {
    val course = createHyperskillCourse()

    withVirtualFileListener(course) {
      val task1 = course.findTask("lesson1", "task1")
      task1.openTaskFileInEditor("src/Task.kt")
      task1.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      myFixture.testAction(PreviousTaskAction())


      task1.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun baz() {}\n")

      withTestDialog(EduTestDialog(Messages.YES)) {
        myFixture.testAction(NextTaskAction())
      }.checkWasShown()
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", """
              fun baz() {}
              fun foo() {}
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


  fun `test propagate new files next task (next)`() {
    val course = createHyperskillCourse()

    val task1 = course.findTask("lesson1", "task1")
    val task2 = course.findTask("lesson1", "task2")

    withVirtualFileListener(course) {
      GeneratorUtils.createChildFile(rootDir, "lesson1/task/src/Bar.kt", "fun bar() {}")
      task1.openTaskFileInEditor("src/Task.kt")
      task1.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())
    }

    assertThat(task1.taskFiles.keys, hasItem("src/Bar.kt"))
    assertThat(task2.taskFiles.keys, hasItem("src/Bar.kt"))

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", """
              fun foo() {}
            """)
            file("Bar.kt", """
              fun bar() {}
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

  fun `test removed files in solution`() {
    val course = createHyperskillCourse()

    val task1 = course.findTask("lesson1", "task1")
    val task2 = course.findTask("lesson1", "task2")

    // emulate solution with removed file
    val externalState = task2.taskFiles.mapValues { it.value.text } - "src/Baz.kt"
    val frameworkLessonManager = FrameworkLessonManager.getInstance(project)
    frameworkLessonManager.saveExternalChanges(task2, externalState)

    withVirtualFileListener(course) {
      withTestDialog(EduTestDialog(Messages.NO)) {
        task1.openTaskFileInEditor("src/Task.kt")
        task1.status = CheckStatus.Solved
        myFixture.testAction(NextTaskAction())
      }
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", """
              fun foo() {}
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

  fun `test removed file`() {
    val course = createHyperskillCourse()

    val task1 = course.findTask("lesson1", "task1")
    val task2 = course.findTask("lesson1", "task2")

    withVirtualFileListener(course) {
      withTestDialog(EduTestDialog(Messages.NO)) {
        runWriteAction {
          findFile("lesson1/task/src/Baz.kt").delete(HyperskillNavigationTest::class.java)
        }
        task1.openTaskFileInEditor("src/Task.kt")
        task1.status = CheckStatus.Solved
        myFixture.testAction(NextTaskAction())
      }
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", """
              fun foo() {}
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

    assertThat(task2.taskFiles.keys, not(hasItem("src/Baz.kt")))
  }

  private fun createHyperskillCourse(): Course {
    val course = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::HyperskillCourse
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1") {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
        eduTask("task2") {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests2.kt", "fun tests2() {}")
        }
        eduTask("task3") {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests3.kt", "fun tests3() {}")
        }
      }
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    return course
  }
}
