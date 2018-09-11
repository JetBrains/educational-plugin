package com.jetbrains.edu.learning.coursera

import com.jetbrains.edu.learning.courseFormat.Course

class StartCourseraProgrammingAssignment : ImportLocalCourseAction("Start Coursera Programming Assignment") {

  override fun initCourse(course: Course) {
    super.initCourse(course)
    course.courseType = CourseraNames.COURSE_TYPE
  }
}