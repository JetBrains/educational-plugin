package com.jetbrains.edu.learning.update

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.fileTree
import kotlinx.coroutines.runBlocking
import org.junit.Test

abstract class SectionUpdateTestBase<T : Course> : UpdateTestBase<T>() {
  abstract fun getUpdater(course: Course): SectionUpdater

  protected fun updateSections(remoteCourse: T, isShouldBeUpdated: Boolean = true) {
    val updater = getUpdater(localCourse)
    val updates = runBlocking {
      updater.collect(remoteCourse)
    }
    assertEquals("Updates are " + if (isShouldBeUpdated) "" else "not" + " available", isShouldBeUpdated, updates.isNotEmpty())
    val isUpdateSucceed = runBlocking {
      try {
        updater.update(remoteCourse)
        true
      } catch (e: Exception) {
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
    updateSections(remoteCourse, isShouldBeUpdated = false)

    val expectedStructure = fileTree {
      dir("section1") {
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
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }
}