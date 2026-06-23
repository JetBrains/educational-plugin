package com.jetbrains.edu.learning.newproject.newCourseSettings

import com.jetbrains.edu.learning.courseFormat.Course

/**
 * Represents settings for creating a new course.
 * It is supposed that a user chooses one of the [NewCourseSettings] instances in the New Course dialog.
 * These settings modify the course object. They affect the course generation,
 * for example, they may affect the generated files or templates.
 */
interface NewCourseSettings {
  /**
   * Applies settings to the given course.
   *
   * TODO this method is a rewrite of "CourseProjectGenerator.applySettings". The latter should be removed in EDU-8931
   */
  fun applyToCourse(course: Course)
}