package com.jetbrains.edu.learning.newproject.ui

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseDisplaySettings

class JoinCourseDialog(course: Course) : JoinCourseDialogBase(course, CourseDisplaySettings()) {
  init { init() }
}
