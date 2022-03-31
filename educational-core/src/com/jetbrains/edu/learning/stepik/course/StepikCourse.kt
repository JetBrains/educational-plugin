package com.jetbrains.edu.learning.stepik.course

import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.copyAs
import com.jetbrains.edu.learning.stepik.StepikNames

/**
 * Specific stepik course created via `StartStepikCourseAction`.
 * We do not push this kind of courses to the stepik.
 * Stepik courses do not contain pycharm tasks.
 */
class StepikCourse : EduCourse() {
  override val itemType: String = StepikNames.STEPIK_TYPE
  override val isViewAsEducatorEnabled: Boolean
    get() = ApplicationManager.getApplication().isInternal
}

fun stepikCourseFromRemote(remoteCourse: EduCourse): StepikCourse {
  val stepikCourse = remoteCourse.copyAs(StepikCourse::class.java)
  stepikCourse.description = remoteCourse.description + descriptionNote(stepikCourse.id)
  return stepikCourse
}

private fun descriptionNote(courseId: Int): String =
  """<br/><br/>Learn more at <a href="${StepikNames.STEPIK_DEFAULT_URL}/course/$courseId">${StepikNames.STEPIK_DEFAULT_URL}/course/$courseId</a>"""
