package com.jetbrains.edu.learning.coursera

import com.intellij.util.xmlb.XmlSerializer
import com.jetbrains.edu.learning.courseFormat.Course

class CourseraCourse : Course() {
  override fun getItemType(): String = CourseraNames.COURSE_TYPE
}

// TODO: add course type to Json and deserialize correctly
fun courseraCourseFromLocal(course: Course): CourseraCourse {
  val element = XmlSerializer.serialize(course)
  val courseraCourse = XmlSerializer.deserialize(element, CourseraCourse::class.java)
  courseraCourse.init(null, null, true)
  return courseraCourse
}
