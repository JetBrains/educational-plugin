package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.courseFormat.Course


interface CourseSetListener {
  fun courseSet(course: Course)
}