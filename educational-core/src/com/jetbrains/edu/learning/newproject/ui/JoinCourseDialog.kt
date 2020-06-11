package com.jetbrains.edu.learning.newproject.ui

import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseDisplaySettings
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView

class JoinCourseDialog(course: Course) : JoinCourseDialogBase(course, CourseDisplaySettings()) {
  init {
    init()
    UIUtil.setBackgroundRecursively(rootPane, TaskDescriptionView.getTaskDescriptionBackgroundColor())
  }
}
