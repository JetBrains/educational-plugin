package com.jetbrains.edu.learning.actions.navigate

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.actions.TaskNavigationAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.navigation.NavigationUtils

// Note, `CodeInsightTestFixture#type` can trigger completion (e.g. it inserts paired `"`)
class FrameworkLessonNavigationTest : NavigationTestBase() {

  fun `test next`() {
    val course = createFrameworkCourse()

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      task.openTaskFileInEditor("fizz.kt", 0)
      myFixture.type("123")
      task.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizz.kt", """
            fun fizz() = 123
          """)
          file("buzz.kt", """
            fun buzz() = TODO()
          """)
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
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test next next`() {
    val course = createFrameworkCourse()
    val task = course.findTask("lesson1", "task1")

    withVirtualFileListener(course) {
      task.openTaskFileInEditor("fizz.kt", 0)
      myFixture.type("123")
      task.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("buzz.kt", 0)
      myFixture.type("456")
      task2.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizzBuzz.kt", """
            fun fizzBuzz() = 123 + 456
          """)
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
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test next prev`() {
    val course = createFrameworkCourse()

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      task.openTaskFileInEditor("fizz.kt", 0)
      myFixture.type("123")
      task.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())

      myFixture.testAction(PreviousTaskAction())
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizz.kt", """
            fun fizz() = 123
          """)
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
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test correctly process placeholder offsets`() {
    val course = courseWithFiles {
      frameworkLesson {
        eduTask {
          taskFile("fizz.kt", """
          fun fizzz() = <p>TODO()</p>
          fun buzz() = <p>TODO()</p>
        """)
        }
        eduTask {
          taskFile("fizz.kt", """
          fun fizzz() = <p>TODO()</p>
          fun buzz() = <p>TODO()</p>
        """) {
            placeholder(0, dependency = "lesson1#task1#fizz.kt#1")
            placeholder(1, dependency = "lesson1#task1#fizz.kt#2")
          }
        }
      }
    }

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      task.openTaskFileInEditor("fizz.kt", placeholderIndex = 0)
      myFixture.type("12345678")
      task.openTaskFileInEditor("fizz.kt", placeholderIndex = 1)
      myFixture.type("90")
      task.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())

      myFixture.testAction(PreviousTaskAction())
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizz.kt", """
            fun fizzz() = 12345678
            fun buzz() = 90
          """)
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

  fun `test opened files`() {
    val course = createFrameworkCourse()

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      task.openTaskFileInEditor("fizz.kt", 0)
      myFixture.type("123")
      task.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())
    }

    val openFiles = FileEditorManager.getInstance(project).openFiles
    assertEquals(1, openFiles.size)
    assertEquals("buzz.kt", openFiles[0].name)
  }

  fun `test navigation to unsolved task`() {
    val course = createFrameworkCourse()

    val task1 = course.findTask("lesson1", "task1")
    withVirtualFileListener(course) {
      // go to the third task without solving prev tasks
      task1.openTaskFileInEditor("fizz.kt", 0)
      myFixture.testAction(NextTaskAction())

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("fizz.kt", 0)
      myFixture.testAction(NextTaskAction())
    }

    fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizzBuzz.kt", """
            fun fizzBuzz() = TODO() + TODO()
          """)
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
    }.assertEquals(rootDir, myFixture)

    // Emulate user actions with unsolved dependencies notification
    // and navigate to the first unsolved task
    NavigationUtils.navigateToTask(project, task1, course.lessons[0].getTask("task3"))

    fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizz.kt", """
            fun fizz() = TODO()
          """)
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
    }.assertEquals(rootDir, myFixture)
  }

  fun `test navigation in CC mode`() {
    val course = createFrameworkCourse(CCUtils.COURSE_MODE)
    val task1 = course.findTask("lesson1", "task1")
    val task2 = course.findTask("lesson1", "task2")
    val task3 = course.findTask("lesson1", "task3")

    withVirtualFileListener(course) {
      doTest(PreviousTaskAction(), task1) { task2.openTaskFileInEditor("buzz.kt") }
      doTest(NextTaskAction(), task3) { task2.openTaskFileInEditor("buzz.kt") }
    }
  }

  fun `test save student changes`() {
    val course = createFrameworkCourse()
    val task = course.findTask("lesson1", "task1")
    withVirtualFileListener(course) {
      task.openTaskFileInEditor("fizz.kt", 0)
      myFixture.type("123")
      myFixture.editor.caretModel.moveToOffset(0)
      myFixture.type("fun foo() {}\n")
      task.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())

      myFixture.testAction(PreviousTaskAction())
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizz.kt", """
            fun foo() {}
            fun fizz() = 123
          """)
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
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test save student changes 2`() {
    val course = createFrameworkCourse()
    val task = course.findTask("lesson1", "task1")
    withVirtualFileListener(course) {
      task.openTaskFileInEditor("fizz.kt", 0)
      myFixture.type("123")
      task.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("buzz.kt", 0)
      myFixture.type("456")
      myFixture.editor.caretModel.moveToOffset(0)
      myFixture.type("fun bar() {}\n")
      myFixture.testAction(PreviousTaskAction())

      myFixture.testAction(NextTaskAction())
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizz.kt", """
            fun fizz() = 123
          """)
          file("buzz.kt", """
            fun bar() {}
            fun buzz() = 456
          """)
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
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test do not propagate user created files`() {
    val course = createFrameworkCourse()
    val task = course.findTask("lesson1", "task1")
    withVirtualFileListener(course) {
      GeneratorUtils.createChildFile(rootDir, "lesson1/task/foo.kt", "fun foo() {}")
      task.openTaskFileInEditor("fizz.kt")
      task.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("buzz.kt")
      myFixture.testAction(PreviousTaskAction())
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizz.kt", """
            fun fizz() = TODO()
          """)
          file("foo.kt", """
            fun foo() {}
          """)
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
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test save user created files`() {
    val course = createFrameworkCourse()
    val task = course.findTask("lesson1", "task1")
    withVirtualFileListener(course) {
      GeneratorUtils.createChildFile(rootDir, "lesson1/task/foo.kt", "fun foo() {}")
      task.openTaskFileInEditor("fizz.kt")
      task.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizz.kt", """
            fun fizz() = TODO()
          """)
          file("buzz.kt", """
            fun buzz() = TODO()
          """)
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
    }
    fileTree.assertEquals(rootDir, myFixture)
  }


  private inline fun doTest(action: TaskNavigationAction, expectedTask: Task, init: () -> Unit) {
    init()
    myFixture.testAction(action)
    val currentFile = FileEditorManagerEx.getInstanceEx(myFixture.project).currentFile ?: error("Can't find current file")
    val task = EduUtils.getTaskForFile(myFixture.project, currentFile) ?: error("Can't find task for $currentFile")
    check(expectedTask == task) {
      "Expected ${expectedTask.name}, found ${task.name}"
    }
  }

  private fun createFrameworkCourse(courseMode: String = EduNames.STUDY): Course = courseWithFiles(courseMode = courseMode) {
    frameworkLesson {
      eduTask {
        taskFile("fizz.kt", """
          fun fizz() = <p>TODO()</p>
        """)
      }
      eduTask {
        taskFile("fizz.kt", """
          fun fizz() = <p>TODO()</p>
        """) {
          placeholder(0, dependency = "lesson1#task1#fizz.kt#1", isVisible = false)
        }
        taskFile("buzz.kt", """
          fun buzz() = <p>TODO()</p>
        """)
      }
      eduTask {
        taskFile("fizzBuzz.kt", """
          fun fizzBuzz() = <p>TODO()</p> + <p>TODO()</p>
        """) {
          placeholder(0, dependency = "lesson1#task2#fizz.kt#1")
          placeholder(1, dependency = "lesson1#task2#buzz.kt#1")
        }
      }
    }
  }
}
