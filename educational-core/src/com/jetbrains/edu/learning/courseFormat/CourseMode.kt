package com.jetbrains.edu.learning.courseFormat

enum class CourseMode {
  STUDY {
    override fun toString(): String = "Study"
  },
  COURSE_MODE {
    override fun toString(): String = "Course Creator"
  };
}