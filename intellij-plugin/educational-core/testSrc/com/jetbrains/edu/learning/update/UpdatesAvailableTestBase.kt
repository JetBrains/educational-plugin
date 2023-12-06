package com.jetbrains.edu.learning.update

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings

abstract class UpdatesAvailableTestBase<T : Course> : CourseGenerationTestBase<EmptyProjectSettings>() {
  protected lateinit var localCourse: T

  override val defaultSettings: EmptyProjectSettings get() = EmptyProjectSettings

  abstract fun initiateLocalCourse()

  override fun runInDispatchThread(): Boolean = false
}