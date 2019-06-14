package com.jetbrains.edu.learning.coursera

import com.jetbrains.edu.learning.courseFormat.Course

class CourseraCourse : Course() {
  override fun getItemType(): String = CourseraNames.COURSE_TYPE
}

// TODO: add course type to Json and deserialize correctly
fun courseraCourseFromLocal(course: Course): CourseraCourse {
  return course.copyAs(CourseraCourse::class.java)
}
