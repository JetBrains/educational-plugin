package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.jetbrains.edu.learning.courseFormat.Course

data class CourseBindData(
  val course: Course,
  val displaySettings: CourseDisplaySettings = CourseDisplaySettings(),
)
