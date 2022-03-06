package com.jetbrains.edu.learning.courseFormat

enum class CourseMode {
  STUDENT,
  EDUCATOR;

  /**
   * String constants are different from variable names to provide backward compatibility with old courses
   */
  override fun toString(): String = when (this) {
    STUDENT -> STUDY
    EDUCATOR -> COURSE_CREATOR
  }

  companion object {
    private const val STUDY = "Study"
    private const val COURSE_CREATOR = "Course Creator"

    fun String.toCourseMode(): CourseMode? = when (this) {
      STUDY -> STUDENT
      COURSE_CREATOR -> EDUCATOR
      else -> null
    }
  }
}