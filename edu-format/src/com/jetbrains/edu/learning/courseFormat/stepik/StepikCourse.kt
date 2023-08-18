package com.jetbrains.edu.learning.courseFormat.stepik

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.STEPIK

/**
 * Specific stepik course created via `StartStepikCourseAction`.
 * We do not push this kind of courses to the stepik.
 * Stepik courses do not contain pycharm tasks.
 */
class StepikCourse : EduCourse() {
  var isAdaptive: Boolean = false

  override val itemType: String = STEPIK
}
