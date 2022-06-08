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
  var isAdaptive: Boolean = false

  override val itemType: String = StepikNames.STEPIK_TYPE
  override val isViewAsEducatorEnabled: Boolean
    get() = ApplicationManager.getApplication().isInternal
}
