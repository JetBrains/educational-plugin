package com.jetbrains.edu.learning.newproject.ui.dialogs

import com.intellij.openapi.Disposable
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseBindData
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseDisplaySettings
import com.jetbrains.edu.learning.newproject.CourseCreationInfo
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.errors.ErrorState
import javax.swing.JComponent

open class JoinCourseDialog(
  course: Course,
  settings: CourseDisplaySettings = CourseDisplaySettings()
) : OpenCourseDialogBase() {
  private val coursePanel: CoursePanel = JoinCoursePanel(disposable)

  init {
    super.init()
    title = course.name
    coursePanel.bindCourse(CourseBindData(course, settings))
    coursePanel.preferredSize = JBUI.size(500, 530)
  }

  override fun createCenterPanel(): JComponent = coursePanel

  protected open fun isToShowError(errorState: ErrorState): Boolean = true

  private inner class JoinCoursePanel(parentDisposable: Disposable) : CoursePanel(parentDisposable, true) {
    override fun joinCourseAction(info: CourseCreationInfo, mode: CourseMode) {
      CoursesPlatformProvider.joinCourse(info, mode, this) {
        setError(it)
      }
    }

    override fun showError(errorState: ErrorState) {
      if (isToShowError(errorState)) {
        super.showError(errorState)
      }
    }
  }
}
