package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.OpenCourseAction

abstract class OpenCourseDialogBase : DialogWrapper(true) {

  protected open val allowViewAsEducatorAction: Boolean get() = true
  protected open val openCourseActionName: String get() = "Join"

  abstract val courseInfo: CourseInfo

  data class CourseInfo(val course: Course?, val location: String, val projectSettings: Any)

  abstract fun setError(error: ErrorState)

  override fun createDefaultActions() {
    super.createDefaultActions()
    myOKAction = OpenCourseAction(openCourseActionName, this, allowViewAsEducatorAction)
  }

  fun setEnabledViewAsEducator(enabled: Boolean) {
    (myOKAction as? OpenCourseAction)?.viewAsEducatorAction?.isEnabled = enabled
  }
}
