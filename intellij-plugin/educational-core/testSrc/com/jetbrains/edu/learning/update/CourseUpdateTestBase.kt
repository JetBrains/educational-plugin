package com.jetbrains.edu.learning.update

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.fileTree
import kotlinx.coroutines.runBlocking
import org.junit.Test

abstract class CourseUpdateTestBase<T : Course> : UpdateTestBase<T>() {
  abstract fun getUpdater(course: Course): CourseUpdater

  protected fun updateCourse(remoteCourse: T, isShouldBeUpdated: Boolean = true) {
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
        LOG.error(e)
        false
      }
    }
    if (isShouldBeUpdated) {
      assertTrue("Update failed", isUpdateSucceed)
    }
  }

  @Test
  open fun `test nothing to update`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse { }
    updateCourse(remoteCourse, isShouldBeUpdated = false)

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson1") {
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
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }
}