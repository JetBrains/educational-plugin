package com.jetbrains.edu.sql

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class SqlCourseProjectGenerator(
  builder: SqlCourseBuilder,
  course: Course
) : CourseProjectGenerator<Unit>(builder, course)
