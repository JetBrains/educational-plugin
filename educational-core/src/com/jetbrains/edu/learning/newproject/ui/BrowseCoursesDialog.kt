package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.impl.coroutineDispatchingContext
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import javax.swing.JComponent
import kotlin.coroutines.CoroutineContext

class BrowseCoursesDialog : OpenCourseDialogBase(), CoroutineScope {
  val panel = CoursesPanelWithTabs(this)

  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = AppUIExecutor.onUiThread(ModalityState.any()).coroutineDispatchingContext() + job

  init {
    title = "Select Course"
    init()
    UIUtil.setBackgroundRecursively(rootPane, TaskDescriptionView.getTaskDescriptionBackgroundColor())
    panel.setSidePaneBackground()
    panel.addCourseValidationListener(object : CoursesPanel.CourseValidationListener {
      override fun validationStatusChanged(canStartCourse: Boolean) {
        isOKActionEnabled = canStartCourse
        setEnabledViewAsEducator(panel.selectedCourse?.isViewAsEducatorEnabled ?: true)
      }
    })

    Disposer.register(disposable, Disposable { job.cancel() })
    panel.loadCourses(this)
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return panel
  }

  override val courseInfo: CourseInfo
    get() = CourseInfo(panel.selectedCourse ?: error("The course was not selected"), { panel.locationString }, { panel.projectSettings })

  override fun createCenterPanel(): JComponent = panel

  override fun setError(error: ErrorState) {
    panel.setError(error)
  }
}
