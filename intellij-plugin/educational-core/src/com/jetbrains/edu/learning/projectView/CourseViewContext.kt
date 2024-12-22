package com.jetbrains.edu.learning.projectView

import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator

data class CourseViewContext(
  val course: Course,
  val configurator: EduConfigurator<*>,
  val additionalFiles: Set<String>
) {

  companion object {
    fun create(course: Course): CourseViewContext? {
      val configurator = course.configurator ?: return null
      val additionalFiles = course.additionalFiles.map { it.name }.toSet()

      return CourseViewContext(course, configurator, additionalFiles)
    }
  }
}
