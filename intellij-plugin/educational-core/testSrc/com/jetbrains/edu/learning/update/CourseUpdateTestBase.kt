package com.jetbrains.edu.learning.update

import com.jetbrains.edu.learning.courseFormat.Course
import kotlinx.coroutines.runBlocking

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
}