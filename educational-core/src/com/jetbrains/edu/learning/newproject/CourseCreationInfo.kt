package com.jetbrains.edu.learning.newproject

import com.jetbrains.edu.learning.courseFormat.Course

data class CourseCreationInfo(
  val course: Course,
  val location: String?,
  val projectSettings: EduProjectSettings?
)
