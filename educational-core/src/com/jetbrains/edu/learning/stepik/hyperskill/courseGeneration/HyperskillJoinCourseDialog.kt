package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialogBase
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseDisplaySettings
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class HyperskillJoinCourseDialog(course: HyperskillCourse) : JoinCourseDialogBase(course, CourseDisplaySettings(showTagsPanel = false,
                                                                                                                showInstructorField = false)) {
  override val openCourseActionName: String get() = "Continue"

  init { init() }
}
