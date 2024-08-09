package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.Disposable
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.CourseVisibility.LocalVisibility
import com.jetbrains.edu.learning.marketplace.newProjectUI.MarketplaceCoursePanel
import com.jetbrains.edu.learning.newproject.CourseCreationInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseBindData
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseDisplaySettings
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.errors.ErrorState
import com.jetbrains.edu.learning.statistics.DownloadCourseContext
import com.jetbrains.edu.learning.statistics.DownloadCourseContext.IDE_UI
import javax.swing.JComponent

open class JoinCourseDialog(
  protected val course: Course,
  protected val settings: CourseDisplaySettings = CourseDisplaySettings(),
  private val downloadCourseContext: DownloadCourseContext = IDE_UI
) : OpenCourseDialogBase() {

  init {
    super.init()
    title = course.name + course.ltiLaunchId?.let { " For LTI launch" }.orEmpty()
  }

  override fun createCenterPanel(): JComponent {
    val panel = createCoursePanel()
    panel.bindCourse(CourseBindData(course, settings))
    panel.preferredSize = JBUI.size(500, 530)
    return panel
  }

  protected open fun createCoursePanel(): CoursePanel {
    return when {
      course.isMarketplace && course.visibility != LocalVisibility -> MarketplaceCoursePanel(disposable, downloadCourseContext)
      else -> JoinCoursePanel(disposable)
    }
  }

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
