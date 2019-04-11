package com.jetbrains.edu.learning.stepik.course

import com.intellij.util.xmlb.XmlSerializer
import com.jetbrains.edu.learning.courseFormat.CourseCompatibility
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.stepik.StepikNames

/**
 * Specific stepik course created via `StartStepikCourseAction`.
 * This course can't be open with "View as Educator".
 * We do not push this kind of courses to the stepik.
 * Stepik courses do not contain pycharm tasks.
 */
class StepikCourse : EduCourse() {
  override fun courseCompatibility(courseInfo: EduCourse) = CourseCompatibility.COMPATIBLE;
}

fun stepikCourseFromRemote(remoteCourse: EduCourse): StepikCourse? {
  val element = XmlSerializer.serialize(remoteCourse)
  val stepikCourse = XmlSerializer.deserialize(element, StepikCourse::class.java)
  stepikCourse.init(null, null, true)
  stepikCourse.type = StepikNames.STEPIK_TYPE
  return stepikCourse
}
