package com.jetbrains.edu.learning.stepik.course

import com.intellij.util.xmlb.XmlSerializer
import com.jetbrains.edu.learning.courseFormat.EduCourse

class StepikCourse : EduCourse()

fun stepikCourseFromRemote(remoteCourse: EduCourse?): StepikCourse? {
  if (remoteCourse == null) return null
  val element = XmlSerializer.serialize(remoteCourse)
  val stepikCourse = XmlSerializer.deserialize(element, StepikCourse::class.java)
  stepikCourse.init(null, null, true)
  return stepikCourse
}
