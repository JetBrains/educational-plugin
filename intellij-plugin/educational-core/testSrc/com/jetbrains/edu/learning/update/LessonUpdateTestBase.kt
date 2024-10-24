package com.jetbrains.edu.learning.update

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.fileTree
import org.junit.Test

abstract class LessonUpdateTestBase<T : Course> : UpdateTestBase<T>() {

  @Test
  fun `test lesson name has been updated`() {
    initiateLocalCourse()

    val newLessonName = "newLessonName"
    val remoteCourse = toRemoteCourse {
      lessons[0].name  = newLessonName
    }

    updateCourse(remoteCourse)

    assertEquals("Lesson name not updated", newLessonName, localCourse.lessons[0].name)

    val expectedStructure = fileTree {
      dir(newLessonName) {
        dir("task1") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test lesson name has been updated and task statuses are saved`() {
    initiateLocalCourse()
    localCourse.lessons[0].taskList.apply {
      first().status = CheckStatus.Solved
      last().status = CheckStatus.Failed
    }

    val newLessonName = "newLessonName"
    val remoteCourse = toRemoteCourse {
      lessons[0].name  = newLessonName
    }

    updateCourse(remoteCourse)

    val taskStatuses = localCourse.lessons[0].taskList.associateBy({ it.name }, { it.status })
    assertEquals("Task statuses not saved", CheckStatus.Solved, taskStatuses["task1"])
    assertEquals("Task statuses not saved", CheckStatus.Failed, taskStatuses["task2"])

    val expectedStructure = fileTree {
      dir(newLessonName) {
        dir("task1") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }
}