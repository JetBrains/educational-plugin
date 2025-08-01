package com.jetbrains.edu.learning.marketplace.courseStorage.course

import com.jetbrains.edu.learning.marketplace.api.EduCourseConnector
import com.jetbrains.edu.learning.stepik.course.ImportCoursePanel

class ImportCourseFromStoragePanel(courseConnector: EduCourseConnector) : ImportCoursePanel(courseConnector, HELP_LABEL) {
  override fun setValidationListener(validationListener: ValidationListener?) {}

  companion object {
    // TODO(clarify help label when we get separate pages for courses)
    private const val HELP_LABEL = "Enter ID of the course from courses storage."
  }
}