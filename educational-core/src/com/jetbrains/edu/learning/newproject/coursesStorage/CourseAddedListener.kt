package com.jetbrains.edu.learning.newproject.coursesStorage

import com.jetbrains.edu.learning.courseFormat.Course

interface CourseAddedListener {
  fun courseAdded(course: Course)
}