package com.jetbrains.edu.learning.marketplace.awsTracks.course

import com.jetbrains.edu.learning.stepik.course.CourseConnector
import com.jetbrains.edu.learning.stepik.course.ImportCoursePanel

class ImportAWSCoursePanel(courseConnector: CourseConnector) : ImportCoursePanel(courseConnector, HELP_LABEL) {
  override fun setValidationListener(validationListener: ValidationListener?) {}

  companion object {
    // TODO(change help label when we have separate pages for AWS courses)
    private const val HELP_LABEL = "Enter ID of the course from AWS courses storage."
  }
}