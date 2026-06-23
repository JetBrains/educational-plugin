package com.jetbrains.edu.learning.newproject.newCourseSettings

import com.jetbrains.edu.learning.courseFormat.Course

/**
 * Represents a set of possible [NewCourseSettings]. A user should choose one on course creation.
 * The structure of the catalog affects the UI. For example, a [List] option may be used with a dropdown list.
 */
sealed interface NewCourseSettingsCatalog<out S: NewCourseSettings> {
  val preferred: S

  interface List<out S: NewCourseSettings> : NewCourseSettingsCatalog<S> {
    val configs: kotlin.collections.List<S>
  }

  /**
   * The empty catalog. It contains only one [preferred] setting, that is no-op.
   * It is supposed that a user has no UI to select from this catalog.
   */
  object Empty : NewCourseSettingsCatalog<NewCourseSettings> {
    override val preferred: NewCourseSettings
      get() = object : NewCourseSettings {
        override fun applyToCourse(course: Course) {
          // do nothing
        }
      }
  }
}