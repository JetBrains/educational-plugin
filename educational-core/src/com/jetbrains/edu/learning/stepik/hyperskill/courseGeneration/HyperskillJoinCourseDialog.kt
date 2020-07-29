package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialogBase
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseDisplaySettings
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class HyperskillJoinCourseDialog(course: HyperskillCourse) : JoinCourseDialogBase(course, CourseDisplaySettings(showTagsPanel = false,
                                                                                                                showInstructorField = false)) {

  init {
    init()
    UIUtil.setBackgroundRecursively(rootPane, MAIN_BG_COLOR)
  }
}
