package com.jetbrains.edu.learning.marketplace.awsTracks.course

import com.jetbrains.edu.learning.marketplace.awsTracks.AWS
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.stepik.course.CourseConnector
import com.jetbrains.edu.learning.stepik.course.ImportCourseDialog

class StartAWSCourseDialog(courseConnector: CourseConnector) : ImportCourseDialog() {
  override val coursePanel = ImportAWSCoursePanel(courseConnector)

  init {
    title = message("dialog.title.start.course", AWS)
    init()
  }
}