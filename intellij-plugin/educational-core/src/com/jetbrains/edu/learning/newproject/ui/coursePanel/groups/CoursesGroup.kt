package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.jetbrains.edu.learning.courseFormat.Course

data class CoursesGroup(val name: String, val courses: List<Course>) {
  constructor(courses: List<Course>) : this("", courses)

  companion object {
    fun fromCourses(courses: List<Course>): List<CoursesGroup> =
      listOf(CoursesGroup(courses = courses))

    fun fromCourses(vararg courses: Course): List<CoursesGroup> =
      listOf(CoursesGroup(courses = courses.toList()))
  }
}