package com.jetbrains.edu.learning.newproject.ui

import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseDisplaySettings
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR

class JoinCourseDialog(course: Course, settings: CourseDisplaySettings = CourseDisplaySettings()) : JoinCourseDialogBase(course, settings) {
  init {
    init()
    UIUtil.setBackgroundRecursively(rootPane, MAIN_BG_COLOR)
  }
}
