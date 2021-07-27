package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.Disposable
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseDisplaySettings
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import javax.swing.JComponent

open class JoinCourseDialog(
  private val course: Course,
  settings: CourseDisplaySettings = CourseDisplaySettings()
) : OpenCourseDialogBase() {
  private val coursePanel: CoursePanel = JoinCoursePanel(disposable)

  init {
    super.init()
    title = course.name
    coursePanel.bindCourse(course, settings)
    coursePanel.preferredSize = JBUI.size(500, 530)
  }

  override val courseInfo: CourseInfo
    get() = CourseInfo(course, { coursePanel.locationString }, { coursePanel.languageSettings })

  override fun createCenterPanel(): JComponent = coursePanel

  protected open fun isToShowError(errorState: ErrorState): Boolean = true

  private inner class JoinCoursePanel(parentDisposable: Disposable) : CoursePanel(parentDisposable, true) {
    override fun joinCourseAction(info: CourseInfo, mode: CourseMode) {
      CoursesPlatformProvider.joinCourse(CourseInfo(this@JoinCourseDialog.course, { locationString }, { languageSettings }),
                                         mode, this) {
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
