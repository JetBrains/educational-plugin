package com.jetbrains.edu.learning.update

import com.jetbrains.edu.learning.actions.navigate.NavigationTestBase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.copy
import com.jetbrains.edu.learning.courseFormat.copyFileContents

abstract class UpdateTestBase<T : Course> : NavigationTestBase() {
  protected lateinit var localCourse: T

  abstract fun initiateLocalCourse()

  override fun runInDispatchThread(): Boolean = false

  protected fun toRemoteCourse(changeCourse: T.() -> Unit): T =
    localCourse.copy().apply {
      additionalFiles = localCourse.additionalFiles
      copyFileContents(localCourse, this)
      init(false)
      changeCourse()
    }
}