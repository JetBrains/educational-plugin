package com.jetbrains.edu.learning.stepik.course

import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikNames

class ImportStepikCourseDialog(courseConnector: CourseConnector) : ImportCourseDialog() {
  override val coursePanel: ImportCoursePanel = ImportStepikCoursePanel(courseConnector)

  init {
    title = EduCoreBundle.message("dialog.title.start.course", StepikNames.STEPIK)
    init()
    coursePanel.setValidationListener(object : ImportCoursePanel.ValidationListener {
      override fun onLoggedIn(isLoggedIn: Boolean) {
        isOKActionEnabled = isLoggedIn
      }
    })
  }
}