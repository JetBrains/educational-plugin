package com.jetbrains.edu.learning.actions

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.dirName
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.navigation.NavigationUtils

class FrameworkLessonNavigationTest : EduTestCase() {

  private val rootDir: VirtualFile get() = LightPlatformTestCase.getSourceRoot()

  fun `test next`() {
    val course = createFrameworkCourse()
    val task = course.getLesson("lesson1")?.getTask("task1") ?: error("Can't find `task1` in `lesson1`")
    task.openTaskFileInEditor(rootDir, "fizz.kt", 0)
    myFixture.type("\"Fizz\"")
    task.status = CheckStatus.Solved
    myFixture.testAction(NextTaskAction())

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizz.kt", """
            fn fizz() = "Fizz"
          """)
          file("buzz.kt", """
            fn buzz() = TODO()
          """)
        }
      }
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test next next`() {
    val course = createFrameworkCourse()
    val task = course.getLesson("lesson1")?.getTask("task1") ?: error("Can't find `task1` in `lesson1`")
    task.openTaskFileInEditor(rootDir, "fizz.kt", 0)
    myFixture.type("\"Fizz\"")
    task.status = CheckStatus.Solved
    myFixture.testAction(NextTaskAction())

    val task2 = course.getLesson("lesson1")?.getTask("task2") ?: error("Can't find `task2` in `lesson1`")
    task2.openTaskFileInEditor(rootDir, "buzz.kt", 0)
    myFixture.type("\"Buzz\"")
    task2.status = CheckStatus.Solved
    myFixture.testAction(NextTaskAction())

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizzBuzz.kt", """
            fn fizzBuzz() = "Fizz" + "Buzz"
          """)
        }
      }
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test next prev`() {
    val course = createFrameworkCourse()
    val task = course.getLesson("lesson1")?.getTask("task1") ?: error("Can't find `task1` in `lesson1`")
    task.openTaskFileInEditor(rootDir, "fizz.kt", 0)
    myFixture.type("\"Fizz\"")
    task.status = CheckStatus.Solved
    myFixture.testAction(NextTaskAction())

    myFixture.testAction(PreviousTaskAction())

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizz.kt", """
            fn fizz() = "Fizz"
          """)
        }
      }
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test correctly process placeholder offsets`() {
    val course = courseWithFiles {
      lesson(isFramework = true) {
        eduTask {
          taskFile("fizz.kt", """
          fn fizzz() = <p>TODO()</p>
          fn buzz() = <p>TODO()</p>
        """)
        }
        eduTask {
          taskFile("fizz.kt", """
          fn fizzz() = <p>TODO()</p>
          fn buzz() = <p>TODO()</p>
        """) {
            placeholder(0, dependency = "lesson1#task1#fizz.kt#1")
            placeholder(1, dependency = "lesson1#task1#fizz.kt#2")
          }
        }
      }
    }
    val task = course.getLesson("lesson1")?.getTask("task1") ?: error("Can't find `task1` in `lesson1`")
    task.openTaskFileInEditor(rootDir, "fizz.kt", placeholderIndex = 0)
    myFixture.type("\"Fizzz\"")
    task.openTaskFileInEditor(rootDir, "fizz.kt", placeholderIndex = 1)
    myFixture.type("\"Buzz\"")
    task.status = CheckStatus.Solved
    myFixture.testAction(NextTaskAction())

    myFixture.testAction(PreviousTaskAction())

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizz.kt", """
            fn fizzz() = "Fizzz"
            fn buzz() = "Buzz"
          """)
        }
      }
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test opened files`() {
    val course = createFrameworkCourse()
    val task = course.getLesson("lesson1")?.getTask("task1") ?: error("Can't find `task1` in `lesson1`")
    task.openTaskFileInEditor(rootDir, "fizz.kt", 0)
    myFixture.type("\"Fizz\"")
    task.status = CheckStatus.Solved
    myFixture.testAction(NextTaskAction())

    val openFiles = FileEditorManager.getInstance(project).openFiles
    assertEquals(1, openFiles.size)
    assertEquals("buzz.kt", openFiles[0].name)
  }

  fun `test navigation to unsolved task`() {
    val course = createFrameworkCourse()

    // go to the third task without solving prev tasks
    val task1 = course.getLesson("lesson1")?.getTask("task1") ?: error("Can't find `task1` in `lesson1`")
    task1.openTaskFileInEditor(rootDir, "fizz.kt", 0)
    myFixture.testAction(NextTaskAction())

    val task2 = course.getLesson("lesson1")?.getTask("task2") ?: error("Can't find `task2` in `lesson1`")
    task2.openTaskFileInEditor(rootDir, "fizz.kt", 0)
    myFixture.testAction(NextTaskAction())

    fileTree {
      dir("lesson1") {
        dir("task") {
          file("fizzBuzz.kt", """
            fn fizzBuzz() = TODO() + TODO()
          """)
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
            fn fizz() = TODO()
          """)
        }
      }
    }.assertEquals(rootDir, myFixture)
  }

  private fun createFrameworkCourse(): Course = courseWithFiles {
    lesson(isFramework = true) {
      eduTask {
        taskFile("fizz.kt", """
          fn fizz() = <p>TODO()</p>
        """)
      }
      eduTask {
        taskFile("fizz.kt", """
          fn fizz() = <p>TODO()</p>
        """) {
          placeholder(0, dependency = "lesson1#task1#fizz.kt#1")
        }
        taskFile("buzz.kt", """
          fn buzz() = <p>TODO()</p>
        """)
      }
      eduTask {
        taskFile("fizzBuzz.kt", """
          fn fizzBuzz() = <p>TODO()</p> + <p>TODO()</p>
        """) {
          placeholder(0, dependency = "lesson1#task2#fizz.kt#1")
          placeholder(1, dependency = "lesson1#task2#buzz.kt#1")
        }
      }
    }
  }

  private fun Task.openTaskFileInEditor(baseDir: VirtualFile, taskFilePath: String, placeholderIndex: Int? = null) {
    val taskFile = getTaskFile(taskFilePath) ?: error("Can't find task file `$taskFilePath` in `$name`")
    val path = "lesson${lesson.index}/$dirName/$taskFilePath"
    val file = baseDir.findFileByRelativePath(path) ?: error("Can't find `$path` file")
    myFixture.openFileInEditor(file)
    if (placeholderIndex != null) {
      val placeholder = taskFile.answerPlaceholders[placeholderIndex]
      myFixture.editor.selectionModel.setSelection(placeholder.offset, placeholder.offset + placeholder.realLength)
    }
  }
}
