package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.OpenCourseAction

abstract class OpenCourseDialogBase : DialogWrapper(true) {

  init {
    @Suppress("LeakingThis")
    myOKAction = OpenCourseAction(this)
  }

  abstract val courseInfo: CourseInfo

  data class CourseInfo(val course: Course?, val location: String, val projectSettings: Any)
}
