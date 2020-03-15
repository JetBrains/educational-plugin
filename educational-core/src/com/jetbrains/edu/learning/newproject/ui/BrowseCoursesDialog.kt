package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import javax.swing.JComponent

class BrowseCoursesDialog(val courses: List<Course>, customToolbarActions: DefaultActionGroup? = null) : OpenCourseDialogBase() {
  val panel = CoursesPanel(courses, this, customToolbarActions) { setEnabledViewAsEducator(it) }

  init {
    title = "Select Course"
    init()
    UIUtil.setBackgroundRecursively(rootPane, TaskDescriptionView.getTaskDescriptionBackgroundColor())
    panel.addCourseValidationListener(object : CoursesPanel.CourseValidationListener {
      override fun validationStatusChanged(canStartCourse: Boolean) {
        isOKActionEnabled = canStartCourse
        setEnabledViewAsEducator(panel.selectedCourse?.isViewAsEducatorEnabled ?: true)
      }
    })
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return panel
  }

  override val courseInfo: CourseInfo
    get() = CourseInfo(panel.selectedCourse ?: error("The course was not selected"), { panel.locationString }, { panel.projectSettings })

  override fun createCenterPanel(): JComponent = panel

  override fun setError(error: ErrorState) {
    panel.updateErrorInfo(error)
  }
}
