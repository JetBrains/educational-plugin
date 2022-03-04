package com.jetbrains.edu.learning.courseFormat

enum class CourseMode {
  STUDENT,
  EDUCATOR;

  /**
   * String constants are different from variable names to provide backward compatibility with old courses
   */
  override fun toString(): String = when (this) {
    STUDENT -> "Study"
    EDUCATOR -> "Course Creator"
  }
}