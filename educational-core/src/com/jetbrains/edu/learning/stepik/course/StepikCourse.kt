package com.jetbrains.edu.learning.stepik.course

import com.intellij.util.xmlb.XmlSerializer
import com.jetbrains.edu.learning.courseFormat.RemoteCourse

class StepikCourse : RemoteCourse()

fun stepikCourseFromRemote(remoteCourse: RemoteCourse?): StepikCourse? {
  if (remoteCourse == null) return null
  val element = XmlSerializer.serialize(remoteCourse)
  val stepikCourse = XmlSerializer.deserialize(element, StepikCourse::class.java)
  stepikCourse.init(null, null, true)
  return stepikCourse
}
