package com.jetbrains.edu.learning.marketplace.course

import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.stepik.course.CourseConnector
import com.jetbrains.edu.learning.stepik.course.ImportCourseDialog
import com.jetbrains.edu.learning.stepik.course.ImportCoursePanel

class StartMarketplaceCourseDialog(courseConnector: CourseConnector) : ImportCourseDialog() {
  override val coursePanel: ImportCoursePanel = ImportMarketplaceCoursePanel(courseConnector)

  init {
    title = message("dialog.title.start.course", MARKETPLACE)
    init()
  }
}