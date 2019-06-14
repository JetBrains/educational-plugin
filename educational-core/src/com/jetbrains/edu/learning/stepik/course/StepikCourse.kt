package com.jetbrains.edu.learning.stepik.course

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
  override fun courseCompatibility(courseInfo: EduCourse) = CourseCompatibility.COMPATIBLE
  override fun getItemType(): String = StepikNames.STEPIK_TYPE
}

fun stepikCourseFromRemote(remoteCourse: EduCourse): StepikCourse? {
  return remoteCourse.copyAs(StepikCourse::class.java)
}
