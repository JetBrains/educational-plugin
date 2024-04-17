package com.jetbrains.edu.learning.update

import com.intellij.openapi.diagnostic.thisLogger
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.LessonContainer
import com.jetbrains.edu.learning.fileTree
import kotlinx.coroutines.runBlocking
import org.junit.Test

abstract class LessonUpdateTestBase<T : Course> : UpdateTestBase<T>() {
  abstract fun getUpdater(container: LessonContainer): LessonUpdater

  protected fun updateLessons(
    remoteCourse: T,
    container: LessonContainer? = null,
    remoteContainer: LessonContainer? = null,
    isShouldBeUpdated: Boolean = true
  ) {
    val containerToBeUpdated = container ?: localCourse
    val updater = getUpdater(containerToBeUpdated)
    val containerFromServer = remoteContainer ?: remoteCourse
    val updates = runBlocking {
      updater.collect(containerFromServer)
    }
    assertEquals("Updates are " + if (isShouldBeUpdated) "" else "not" + " available", isShouldBeUpdated, updates.isNotEmpty())
    val isUpdateSucceed = runBlocking {
      try {
        updater.update(containerFromServer)
        true
      }
      catch (e: Exception) {
        thisLogger().error(e)
        false
      }
    }
    if (isShouldBeUpdated) {
      assertTrue("Update failed", isUpdateSucceed)
    }
  }

  @Test
  fun `test nothing to update`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse { }
    updateLessons(remoteCourse, isShouldBeUpdated = false)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests1.kt")
          }
          file("task.html")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests2.kt")
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
  fun `test lesson name has been updated`() {
    initiateLocalCourse()

    val newLessonName = "newLessonName"
    val remoteCourse = toRemoteCourse {
      lessons[0].name  = newLessonName
    }
    updateLessons(remoteCourse)

    val lessonName = getLessons().first().name
    assertEquals("Lesson name not updated", newLessonName, lessonName)

    val expectedStructure = fileTree {
      dir(newLessonName) {
        dir("task1") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests1.kt")
          }
          file("task.html")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests2.kt")
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
    localCourse.lessons.first().taskList.apply {
      first().status = CheckStatus.Solved
      last().status = CheckStatus.Failed
    }

    val newLessonName = "newLessonName"
    val remoteCourse = toRemoteCourse {
      lessons[0].name  = newLessonName
    }
    updateLessons(remoteCourse)

    val taskStatuses: Map<String, CheckStatus> = getLessons().first().taskList.associateBy({ it.name }, { it.status })
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
            file("Tests1.kt")
          }
          file("task.html")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests2.kt")
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