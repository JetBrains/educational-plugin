package com.jetbrains.edu.learning.marketplace.course

import com.jetbrains.edu.learning.stepik.course.CourseConnector
import com.jetbrains.edu.learning.stepik.course.ImportCoursePanel

class ImportMarketplaceCoursePanel(
  courseConnector: CourseConnector
) : ImportCoursePanel(courseConnector, "https://plugins.jetbrains.com/plugin/*") {

  override fun setValidationListener(validationListener: ValidationListener?) {}
}