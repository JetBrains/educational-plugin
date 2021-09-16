package com.jetbrains.edu.learning.actions.navigate

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.ui.Messages
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.edu.learning.EduTestDialog
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.actions.navigate.hyperskill.HyperskillNavigationTest
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.withEduTestDialog
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat

class NonTemplateBasedFrameworkLessonNavigationTest : NavigationTestBase() {

  fun `test propagate user changes to next task (next)`() {
    val course = createFrameworkCourse()

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      task.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      testAction(NextTaskAction.ACTION_ID)
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
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test do not propagate user changes to prev task (next, prev)`() {
    val course = createFrameworkCourse()

    withVirtualFileListener(course) {
      val task1 = course.findTask("lesson1", "task1")
      task1.openTaskFileInEditor("src/Task.kt")
      testAction(NextTaskAction.ACTION_ID)

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      testAction(PreviousTaskAction.ACTION_ID)
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
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test ask user when changes conflict (next, prev, next), select keep changes`() {
    val course = createFrameworkCourse()

    withVirtualFileListener(course) {
      val task1 = course.findTask("lesson1", "task1")
      task1.openTaskFileInEditor("src/Task.kt")
      testAction(NextTaskAction.ACTION_ID)

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      testAction(PreviousTaskAction.ACTION_ID)


      task1.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun baz() {}\n")

      withEduTestDialog(EduTestDialog(Messages.YES)) {
        testAction(NextTaskAction.ACTION_ID)
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
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test do not ask user about conflicts if there isn't user changes (next, prev, next)`() {
    val course = createFrameworkCourse()

    withVirtualFileListener(course) {
      val task1 = course.findTask("lesson1", "task1")
      task1.openTaskFileInEditor("src/Task.kt")
      testAction(NextTaskAction.ACTION_ID)

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      testAction(PreviousTaskAction.ACTION_ID)


      task1.openTaskFileInEditor("src/Task.kt")
      val dialog = withEduTestDialog(EduTestDialog(Messages.NO)) {
        testAction(NextTaskAction.ACTION_ID)
      }

      BasePlatformTestCase.assertNull(dialog.shownMessage)
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
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test ask user when changes conflict (next, prev, next), select replace changes`() {
    val course = createFrameworkCourse()

    withVirtualFileListener(course) {
      val task1 = course.findTask("lesson1", "task1")
      task1.openTaskFileInEditor("src/Task.kt")
      testAction(NextTaskAction.ACTION_ID)

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      testAction(PreviousTaskAction.ACTION_ID)


      task1.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun baz() {}\n")

      withEduTestDialog(EduTestDialog(Messages.NO)) {
        testAction(NextTaskAction.ACTION_ID)
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
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test propagate new files next task (next)`() {
    val course = createFrameworkCourse()

    val task1 = course.findTask("lesson1", "task1")
    val task2 = course.findTask("lesson1", "task2")

    withVirtualFileListener(course) {
      GeneratorUtils.createChildFile(project, rootDir, "lesson1/task/src/Bar.kt", "fun bar() {}")
      task1.openTaskFileInEditor("src/Task.kt")
      testAction(NextTaskAction.ACTION_ID)
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
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test removed files in solution`() {
    val course = createFrameworkCourse()

    val task1 = course.findTask("lesson1", "task1")
    val task2 = course.findTask("lesson1", "task2")

    // emulate solution with removed file
    val externalState = task2.taskFiles.mapValues { it.value.text } - "src/Baz.kt"
    val frameworkLessonManager = FrameworkLessonManager.getInstance(project)
    frameworkLessonManager.saveExternalChanges(task2, externalState)

    withVirtualFileListener(course) {
      withEduTestDialog(EduTestDialog(Messages.YES)) {
        task1.openTaskFileInEditor("src/Task.kt")
        testAction(NextTaskAction.ACTION_ID)
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
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test removed file`() {
    val course = createFrameworkCourse()

    val task1 = course.findTask("lesson1", "task1")
    val task2 = course.findTask("lesson1", "task2")

    withVirtualFileListener(course) {
      withEduTestDialog(EduTestDialog(Messages.NO)) {
        runWriteAction {
          findFile("lesson1/task/src/Baz.kt").delete(HyperskillNavigationTest::class.java)
        }
        task1.openTaskFileInEditor("src/Task.kt")
        testAction(NextTaskAction.ACTION_ID)
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
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)

    assertThat(task2.taskFiles.keys, not(hasItem("src/Baz.kt")))
  }

  fun `test course with git object files`() {
    class GitObjectFile(val name: String, val text: String)

    val gitObjectFiles = listOf(
      GitObjectFile("2731ee243bb1111dd93916bb3296ee7f7e23ef", "eAErKUpNVTA2YTA0MDAzMVEoLNQrqShhaD4jyTnZfZLvoo4JzV1Xn8241cqyGAAhHRBB"),
      GitObjectFile("3a818dc87b9940935b24a5aa93fac00f086bf9", "eAFLyslPUjA0YfBIzcnJVyjPL8pJUVTgAgBQEgas"),
      GitObjectFile("28add5fd4be3bdd2cdb776dfa035cc69956859", "eAFLyslPUjA2ZUjLLCouUcjJzEvlKk5Nzs9LgbBLMjKLIEwFLgBApg59")
    )

    val tests = PlainTextConfigurator.TEST_DIR_NAME
    val taskFileName = "task.txt"

    val course = courseWithFiles {
      frameworkLesson(isTemplateBased = false) {
        eduTask {
          taskFile(taskFileName)
          taskFile(gitObjectFiles[2].name, gitObjectFiles[2].text)
          taskFile("${tests}/${gitObjectFiles[0].name}", gitObjectFiles[0].text)
          taskFile("${tests}/${gitObjectFiles[1].name}", gitObjectFiles[1].text)
        }
        eduTask {
          taskFile(taskFileName)

          // remove task file gitObjectFiles[0]
          // change text of gitObjectFiles[1]
          // add new task file gitObjectFiles[2]
          taskFile("${tests}/${gitObjectFiles[1].name}", gitObjectFiles[0].text)
          taskFile("${tests}/${gitObjectFiles[2].name}", gitObjectFiles[2].text)
        }
      }
    }

    withVirtualFileListener(course) {
      withEduTestDialog(EduTestDialog(Messages.NO)) {
        course.findTask("lesson1", "task1").openTaskFileInEditor("task.txt")

        //create file to test that file created by learner is propagated
        GeneratorUtils.createChildFile(project, rootDir, "lesson1/task/${gitObjectFiles[0].name}", gitObjectFiles[0].text)

        //remove file to test that it is not propagated
        runWriteAction {
          findFile("lesson1/task/${gitObjectFiles[2].name}").delete(NonTemplateBasedFrameworkLessonNavigationTest::class.java)
        }

        testAction(NextTaskAction.ACTION_ID)
      }
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file(taskFileName)
          file(gitObjectFiles[0].name, gitObjectFiles[0].text)
          dir(tests) {
            // file text should be changed
            file(gitObjectFiles[1].name, gitObjectFiles[0].text)
            file(gitObjectFiles[2].name, gitObjectFiles[2].text)
          }
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
    }

    fileTree.assertEquals(rootDir, myFixture)
  }

  private fun createFrameworkCourse(): Course = courseWithFiles(
    language = FakeGradleBasedLanguage
  ) {
    frameworkLesson("lesson1", isTemplateBased = false) {
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
  }
}
