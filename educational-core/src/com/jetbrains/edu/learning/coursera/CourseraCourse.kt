package com.jetbrains.edu.learning.coursera

import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle

class CourseraCourse : Course() {
  var submitManually = false

  override fun getItemType(): String = CourseraNames.COURSE_TYPE
  override fun getCheckAction(): CheckAction = CheckAction(if (submitManually) EduCoreBundle.lazyMessage("action.coursera.run.tests.text")
                                                           else EduCoreBundle.lazyMessage("action.coursera.submit.text"))
}

// TODO: change course type in coursera archives for Kotlin and remove this method in 2020
fun courseraCourseFromLocal(course: Course): CourseraCourse {
  if (course is CourseraCourse) return course
  return course.copyAs(CourseraCourse::class.java)
}
