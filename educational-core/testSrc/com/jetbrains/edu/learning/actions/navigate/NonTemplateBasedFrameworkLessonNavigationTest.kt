package com.jetbrains.edu.learning.actions.navigate

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat

class NonTemplateBasedFrameworkLessonNavigationTest : NavigationTestBase() {

  override fun setUp() {
    super.setUp()
    val value = isFeatureEnabled(EduExperimentalFeatures.MARKETPLACE)
    setFeatureEnabled(EduExperimentalFeatures.MARKETPLACE, false)
    Disposer.register(testRootDisposable, Disposable {
      setFeatureEnabled(EduExperimentalFeatures.MARKETPLACE, value)
    })
  }

  fun `test propagate user changes to next task (next)`() {
    val course = createFrameworkCourse()

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      task.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
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
      myFixture.testAction(NextTaskAction())

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      myFixture.testAction(PreviousTaskAction())


      task1.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun baz() {}\n")

      withEduTestDialog(EduTestDialog(Messages.YES)) {
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
      myFixture.testAction(NextTaskAction())

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      myFixture.testAction(PreviousTaskAction())


      task1.openTaskFileInEditor("src/Task.kt")
      val dialog = withEduTestDialog(EduTestDialog(Messages.NO)) {
        myFixture.testAction(NextTaskAction())
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
      myFixture.testAction(NextTaskAction())

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      myFixture.testAction(PreviousTaskAction())


      task1.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun baz() {}\n")

      withEduTestDialog(EduTestDialog(Messages.NO)) {
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
      GeneratorUtils.createChildFile(rootDir, "lesson1/task/src/Bar.kt", "fun bar() {}")
      task1.openTaskFileInEditor("src/Task.kt")
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
