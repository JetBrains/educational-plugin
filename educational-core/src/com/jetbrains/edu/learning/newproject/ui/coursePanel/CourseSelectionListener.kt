package com.jetbrains.edu.learning.newproject.ui.coursePanel

interface CourseSelectionListener {
  fun onCourseSelectionChanged(courseInfo: CourseInfo, courseDisplaySettings: CourseDisplaySettings)
}