package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.edu.learning.newproject.OpenCourseAction
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import javax.swing.Action

private const val CANCEL_BUTTON_TEXT = "Close"

abstract class OpenCourseDialogBase : DialogWrapper(true) {
  protected open val openCourseActionName: String get() = "Join"

  abstract val courseInfo: CourseInfo

  abstract fun setError(error: ErrorState)

  override fun createDefaultActions() {
    super.createDefaultActions()
    myCancelAction.putValue(Action.NAME, CANCEL_BUTTON_TEXT)
    myCancelAction.putValue(Action.DEFAULT, true)
    myOKAction = OpenCourseAction(openCourseActionName, this, true)
  }

  fun setEnabledViewAsEducator(enabled: Boolean) {
    (myOKAction as? OpenCourseAction)?.viewAsEducatorAction?.isEnabled = enabled
  }

  override fun getStyle(): DialogStyle {
    return DialogStyle.COMPACT
  }
}
