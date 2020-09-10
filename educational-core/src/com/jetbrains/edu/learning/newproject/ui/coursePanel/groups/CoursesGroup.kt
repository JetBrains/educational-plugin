package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.jetbrains.edu.learning.courseFormat.Course

class CoursesGroup(val name: String, var courses: List<Course>) {
  constructor(courses: List<Course>) : this ("", courses)
}

fun CoursesGroup.asList() = listOf(this)