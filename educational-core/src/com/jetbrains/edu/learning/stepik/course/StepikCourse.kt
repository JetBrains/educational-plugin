package com.jetbrains.edu.learning.stepik.course

import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.stepik.StepikNames

/**
 * Specific stepik course created via `StartStepikCourseAction`.
 * We do not push this kind of courses to the stepik.
 * Stepik courses do not contain pycharm tasks.
 */
class StepikCourse : EduCourse() {
  override fun getItemType(): String = StepikNames.STEPIK_TYPE
  override fun isViewAsEducatorEnabled(): Boolean = ApplicationManager.getApplication().isInternal
}

fun stepikCourseFromRemote(remoteCourse: EduCourse): StepikCourse? {
  return remoteCourse.copyAs(StepikCourse::class.java)
}
