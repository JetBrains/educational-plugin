package com.jetbrains.edu.learning.update

import com.intellij.openapi.application.runInEdt
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.testAction
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit.MINUTES

abstract class FrameworkLessonsUpdateTest<T : Course> : UpdateTestBase<T>() {
  @Test
  fun `test update`() {
    createCourseWithFrameworkLessons()

    val remoteCourse = toRemoteCourse {
      taskList[0].apply {
        updateDate = Date(MINUTES.toMillis(2))
        descriptionText = "New Description"
      }
      taskList[1].apply {
        updateDate = Date(MINUTES.toMillis(3))
        descriptionFormat = DescriptionFormat.MD
      }
    }
    updateCourse(remoteCourse)

    val tasks = localCourse.lessons[0].taskList
    assertEquals(Date(MINUTES.toMillis(2)), tasks[0].updateDate)
    assertEquals(Date(MINUTES.toMillis(3)), tasks[1].updateDate)
    assertEquals("New Description", tasks[0].descriptionText)
    assertEquals(DescriptionFormat.MD, tasks[1].descriptionFormat)
  }

  @Test
  fun `test do not update when tasks ids changed`() {
    createCourseWithFrameworkLessons()
    val oldTasksList = localCourse.lessons[0].taskList
    val oldDescriptionTextTask1 = oldTasksList[0].descriptionText
    val oldDescriptionTextTask2 = oldTasksList[1].descriptionText

    val remoteCourse = toRemoteCourse {
      taskList[0].apply {
        id = 101
        descriptionText = "New Description"
        descriptionFormat = DescriptionFormat.MD
      }
      taskList[1].apply {
        id = 111
        descriptionText = "New Description"
        descriptionFormat = DescriptionFormat.MD
      }
    }

    updateCourse(remoteCourse, isShouldBeUpdated = false)

    val tasks = localCourse.lessons[0].taskList
    assertEquals(oldDescriptionTextTask1, tasks[0].descriptionText)
    assertEquals(oldDescriptionTextTask2, tasks[1].descriptionText)
    assertEquals(DescriptionFormat.HTML, tasks[1].descriptionFormat)
    assertEquals(DescriptionFormat.HTML, tasks[0].descriptionFormat)
  }

  @Test
  fun `test update unmodified current task`() {
    createCourseWithFrameworkLessons()

    val taskText = "fun foo2() {}"
    val testText = """
      fun test1() {}
      fun test2() {}
    """.trimIndent()
    val remoteCourse = toRemoteCourse {
      taskList[0].apply {
        updateDate = Date(MINUTES.toMillis(2))
        taskFiles["src/Task.kt"]!!.text = taskText
        taskFiles["test/Tests1.kt"]!!.text = testText
      }
    }

    updateCourse(remoteCourse)

    val task = localCourse.taskList[0]
    assertEquals(taskText, task.taskFiles["src/Task.kt"]!!.text)
    assertEquals(testText, task.taskFiles["test/Tests1.kt"]!!.text)

    runInEdtAndWait {
      fileTree {
        dir("lesson1") {
          dir("task") {
            dir("src") {
              file("Task.kt", taskText)
              file("Baz.kt", "fun baz() {}")
            }
            dir("test") {
              file("Tests1.kt", testText)
            }
          }
          dir("task1") {
            file("task.html")
          }
          dir("task2") {
            file("task.html")
          }
        }
        file("build.gradle")
        file("settings.gradle")
      }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
    }
  }

  @Test
  fun `test update modified current task`() {
    createCourseWithFrameworkLessons()

    val taskText = "fun foo2() {}"
    val testText = """
      fun test1() {}
      fun test2() {}
    """.trimIndent()

    val task = localCourse.taskList[0]
    runInEdtAndWait {
      task.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
    }

    val remoteCourse = toRemoteCourse {
      taskList[0].apply {
        updateDate = Date(MINUTES.toMillis(2))
        taskFiles["src/Task.kt"]!!.text = taskText
        taskFiles["test/Tests1.kt"]!!.text = testText
      }
    }

    updateCourse(remoteCourse)

    assertEquals("fun foo() {}", task.taskFiles["src/Task.kt"]!!.text)
    assertEquals(testText, task.taskFiles["test/Tests1.kt"]!!.text)

    runInEdtAndWait {
      fileTree {
        dir("lesson1") {
          dir("task") {
            dir("src") {
              file(
                "Task.kt", """
              fun bar() {}
              fun foo() {}
            """.trimIndent()
              )
              file("Baz.kt", "fun baz() {}")
            }
            dir("test") {
              file("Tests1.kt", testText)
            }
          }
          dir("task1") {
            file("task.html")
          }
          dir("task2") {
            file("task.html")
          }
        }
        file("build.gradle")
        file("settings.gradle")
      }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
    }
  }

  @Test
  fun `test update solved current task`() {
    createCourseWithFrameworkLessons()

    val task = localCourse.taskList[0]
    task.status = CheckStatus.Solved

    val remoteCourse = toRemoteCourse {
      taskList[0].apply {
        updateDate = Date(MINUTES.toMillis(2))
        taskFiles["src/Task.kt"]!!.text = "fun foo2() {}"
        taskFiles["test/Tests1.kt"]!!.text = """
          fun test1() {}
          fun test2() {}
        """.trimIndent()
      }
    }
    updateCourse(remoteCourse)

    assertEquals("fun foo() {}", task.taskFiles["src/Task.kt"]!!.text)
    assertEquals("fun test1() {}", task.taskFiles["test/Tests1.kt"]!!.text)

    runInEdtAndWait {
      fileTree {
        dir("lesson1") {
          dir("task") {
            dir("src") {
              file("Task.kt", "fun foo() {}")
              file("Baz.kt", "fun baz() {}")
            }
            dir("test") {
              file("Tests1.kt", "fun test1() {}")
            }
          }
          dir("task1") {
            file("task.html")
          }
          dir("task2") {
            file("task.html")
          }
        }
        file("build.gradle")
        file("settings.gradle")
      }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
    }
  }

  @Test
  fun `test update unmodified non current task`() {
    createCourseWithFrameworkLessons()

    val task1 = localCourse.taskList[0]
    val task2 = localCourse.taskList[1]

    val taskText = "fun foo2() {}"
    val testText = """
      fun test1() {}
      fun test2() {}
    """.trimIndent()
    val remoteCourse = toRemoteCourse {
      taskList[1].apply {
        updateDate = Date(MINUTES.toMillis(2))
        taskFiles["src/Task.kt"]!!.text = taskText
        taskFiles["test/Tests2.kt"]!!.text = testText
      }
    }
    updateCourse(remoteCourse)

    assertEquals("fun foo() {}", task2.taskFiles["src/Task.kt"]!!.text)
    assertEquals(testText, task2.taskFiles["test/Tests2.kt"]!!.text)

    runInEdtAndWait {
      withVirtualFileListener(localCourse) {
        task1.openTaskFileInEditor("src/Task.kt")
        testAction(NextTaskAction.ACTION_ID)
      }

      fileTree {
        dir("lesson1") {
          dir("task") {
            dir("src") {
              file("Task.kt", "fun foo() {}")
              file("Baz.kt", "fun baz() {}")
            }
            dir("test") {
              file("Tests2.kt", testText)
            }
          }
          dir("task1") {
            file("task.html")
          }
          dir("task2") {
            file("task.html")
          }
        }
        file("build.gradle")
        file("settings.gradle")
      }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
    }
  }

  protected abstract fun createCourseWithFrameworkLessons()

  protected val T.taskList: List<Task> get() = lessons[0].taskList
}