package com.jetbrains.edu.coursecreator.actions.create

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCActionTestCase
import com.jetbrains.edu.coursecreator.actions.CCCreateTask
import com.jetbrains.edu.learning.fileTree

class CCCreateFrameworkTaskTest : CCActionTestCase() {

  private val root: VirtualFile get() = LightPlatformTestCase.getSourceRoot()

  fun `test first task in framework lesson`() {
    val lessonName = "FrameworkLesson"
    val taskName = "Task"
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      frameworkLesson(lessonName)
    }

    Messages.setTestInputDialog { taskName }
    val lessonFile = findFile(lessonName)

    testAction(dataContext(lessonFile), CCCreateTask())

    fileTree {
      dir(lessonName) {
        dir(taskName) {
          file("Task.txt")
          file("Tests.txt")
          file("task.html")
        }
      }
    }.assertEquals(root)

    assertEquals(1, course.lessons[0].taskList.size)
  }

  fun `test new task in framework lesson`() {
    val lessonName = "FrameworkLesson"

    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      frameworkLesson(lessonName) {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "\"Foo\"")
          }
        }
      }
    }

    val newTaskName = "task2"
    Messages.setTestInputDialog { newTaskName }
    val lessonFile = findFile(lessonName)

    testAction(dataContext(lessonFile), CCCreateTask())

    fileTree {
      dir(lessonName) {
        dir("task1") {
          file("Task.kt", "fun foo(): String = \"Foo\"")
          file("task.html")
        }
        dir(newTaskName) {
          file("Task.kt", "fun foo(): String = \"Foo\"")
          file("Tests.txt")
          file("task.html")
        }
      }
    }.assertEquals(root)

    assertEquals(2, course.lessons[0].taskList.size)
    val createdTask = course.lessons[0].taskList[1]
    assertEquals(1, createdTask.taskFiles.size)
    val prevTaskFile = course.lessons[0].taskList[0].taskFiles["Task.kt"] ?: error("")
    val taskFile = createdTask.taskFiles.values.single()
    assertEquals(prevTaskFile.name, taskFile.name)
    assertEquals(1, taskFile.answerPlaceholders.size)
    val placeholder = taskFile.answerPlaceholders[0]
    val targetPlaceholder = placeholder.placeholderDependency?.resolve(course) ?: error("Can't resolve placeholder dependency")
    assertEquals(prevTaskFile.answerPlaceholders[0], targetPlaceholder)
  }

  fun `test new task in the middle of lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      frameworkLesson {
        eduTask {
          taskFile("Task.kt", """
            fun foo(): String = <p>TODO()</p>
            fun bar(): String = <p>TODO()</p>
          """) {
            placeholder(0, "\"Foo\"")
            placeholder(1, "\"Bar\"")
          }
        }
        eduTask {
          taskFile("Foo.kt", """
            fun foo(): String = <p>TODO()</p>
          """) {
            placeholder(0, "\"Foo\"", "lesson1#task1#Task.kt#1")
          }
          taskFile("Bar.kt", """
            fun bar(): String = <p>TODO()</p>
          """) {
            placeholder(0, "\"Bar\"", "lesson1#task1#Task.kt#2")
          }
        }
      }
    }
    val task1 = course.lessons[0].getTask("task1") ?: error("Can't find `task1`")
    val task2 = course.lessons[0].getTask("task2") ?: error("Can't find `task2`")

    val firstTaskFile = findFile("lesson1/task1")
    testAction(dataContext(firstTaskFile), CCTestCreateTask("task1.5", 2))

    val insertedTask = course.lessons[0].getTask("task1.5") ?: error("Can't find `task1.5`")
    val taskFile = insertedTask.getTaskFile("Task.kt") ?: error("Can't find `Task.kt` in `task1.5`")

    // Check that all placeholders of new task refer to prev task
    assertEquals(task1.getTaskFile("Task.kt")!!.answerPlaceholders[0], taskFile.answerPlaceholders[0].placeholderDependency?.resolve(course))
    assertEquals(task1.getTaskFile("Task.kt")!!.answerPlaceholders[1], taskFile.answerPlaceholders[1].placeholderDependency?.resolve(course))

    // Check that all placeholders of next task refer to new task
    assertEquals(taskFile.answerPlaceholders[0], task2.getTaskFile("Foo.kt")?.answerPlaceholders?.get(0)?.placeholderDependency?.resolve(course))
    assertEquals(taskFile.answerPlaceholders[1], task2.getTaskFile("Bar.kt")?.answerPlaceholders?.get(0)?.placeholderDependency?.resolve(course))
  }
}
