package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.EduExperimentalFeatures.NEW_COURSE_UPDATE
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.navigate.NavigationTestBase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.rules.WithExperimentalFeature
import org.junit.Test
import java.util.*

@WithExperimentalFeature(id = NEW_COURSE_UPDATE, value = false)
abstract class FrameworkLessonsUpdateTest<T : Course> : NavigationTestBase() {
  protected lateinit var localCourse: T

  @Test
  fun `test update`() {
    createCourseWithFrameworkLessons()

    updateCourse {
      taskList[0].apply {
        updateDate = Date(100)
        descriptionText = "New Description"
      }
      taskList[1].apply {
        updateDate = Date(1000)
        descriptionFormat = DescriptionFormat.MD
      }
    }

    val tasks = localCourse.lessons[0].taskList
    assertEquals(Date(100), tasks[0].updateDate)
    assertEquals(Date(1000), tasks[1].updateDate)
    assertEquals("New Description", tasks[0].descriptionText)
    assertEquals(DescriptionFormat.MD, tasks[1].descriptionFormat)
  }

  @Test
  fun `test do not update when tasks ids changed`() {
    createCourseWithFrameworkLessons()
    val oldTasksList = localCourse.lessons[0].taskList
    val oldDescriptionTextTask1 = oldTasksList[0].descriptionText
    val oldDescriptionTextTask2 = oldTasksList[1].descriptionText

    updateCourse {
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
    updateCourse {
      taskList[0].apply {
        updateDate = Date(100)
        taskFiles["src/Task.kt"]!!.text = taskText
        taskFiles["test/Tests1.kt"]!!.text = testText
      }
    }

    val task = localCourse.taskList[0]
    assertEquals(taskText, task.taskFiles["src/Task.kt"]!!.text)
    assertEquals(testText, task.taskFiles["test/Tests1.kt"]!!.text)

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

  @Test
  fun `test update modified current task`() {
    createCourseWithFrameworkLessons()

    val taskText = "fun foo2() {}"
    val testText = """
      fun test1() {}
      fun test2() {}
    """.trimIndent()

    val task = localCourse.taskList[0]
    task.openTaskFileInEditor("src/Task.kt")
    myFixture.type("fun bar() {}\n")

    updateCourse {
      taskList[0].apply {
        updateDate = Date(100)
        taskFiles["src/Task.kt"]!!.text = taskText
        taskFiles["test/Tests1.kt"]!!.text = testText
      }
    }

    assertEquals("fun foo() {}", task.taskFiles["src/Task.kt"]!!.text)
    assertEquals(testText, task.taskFiles["test/Tests1.kt"]!!.text)

    fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", """
              fun bar() {}
              fun foo() {}
            """.trimIndent())
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

  @Test
  fun `test update solved current task`() {
    createCourseWithFrameworkLessons()

    val task = localCourse.taskList[0]
    task.status = CheckStatus.Solved

    updateCourse {
      taskList[0].apply {
        updateDate = Date(100)
        taskFiles["src/Task.kt"]!!.text = "fun foo2() {}"
        taskFiles["test/Tests1.kt"]!!.text = """
          fun test1() {}
          fun test2() {}
        """.trimIndent()
      }
    }

    assertEquals("fun foo() {}", task.taskFiles["src/Task.kt"]!!.text)
    assertEquals("fun test1() {}", task.taskFiles["test/Tests1.kt"]!!.text)

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
    updateCourse {
      taskList[1].apply {
        updateDate = Date(100)
        taskFiles["src/Task.kt"]!!.text = taskText
        taskFiles["test/Tests2.kt"]!!.text = testText
      }
    }

    assertEquals("fun foo() {}", task2.taskFiles["src/Task.kt"]!!.text)
    assertEquals(testText, task2.taskFiles["test/Tests2.kt"]!!.text)


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

  protected abstract fun createCourseWithFrameworkLessons()

  protected abstract fun updateCourse(changeCourse: T.() -> Unit)

  protected abstract fun toRemoteCourse(changeCourse: T.() -> Unit): T

  protected val T.taskList: List<Task> get() = lessons[0].taskList
}