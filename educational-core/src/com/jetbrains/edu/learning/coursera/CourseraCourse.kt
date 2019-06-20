package com.jetbrains.edu.learning.coursera

import com.jetbrains.edu.learning.courseFormat.Course

class CourseraCourse : Course() {
  var submitManually = false
  override fun getItemType(): String = CourseraNames.COURSE_TYPE
}

// TODO: change course type in coursera archives for Kotlin and remove this method in 2020
fun courseraCourseFromLocal(course: Course): CourseraCourse {
  if (course is CourseraCourse) return course
  return course.copyAs(CourseraCourse::class.java)
}
